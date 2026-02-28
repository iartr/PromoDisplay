package ru.offerfactory.promodisplay.ad.source.impl.util

import ru.offerfactory.promodisplay.ad.source.impl.domain.models.AdAsset
import ru.offerfactory.promodisplay.logger.AppLogger
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

fun String.hexToByteArray(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in indices step 2) {
        val firstDigit = Character.digit(this[i], 16)
        val secondDigit = Character.digit(this[i + 1], 16)

        require(firstDigit != -1 && secondDigit != -1) { "Invalid hex character" }

        result[i / 2] = ((firstDigit shl 4) + secondDigit).toByte()
    }
    return result
}

private fun ByteArray.toHexStringLower(): String =
    joinToString(separator = "") { b -> "%02x".format(b) }

class AdFileValidator @Inject constructor(
    private val appLogger: AppLogger
) {
    suspend fun checkValidClips(asset: AdAsset): Boolean = runCatching {
        val path = asset.localPath
        if (path.isNullOrBlank()) {
            appLogger.logError(Throwable("Validation failed: localPath is null/blank"))
            return false
        }

        val file = File(path)
        if (!file.exists()) {
            appLogger.logError(Throwable("Validation failed: file not found: ${file.absolutePath}"))
            return false
        }

        val actualSize = file.length()
        if (actualSize != asset.sizeBytes) {
            appLogger.logError(
                Throwable("Validation failed: size mismatch: expected=${asset.sizeBytes}, actual=$actualSize, file=${file.absolutePath}")
            )
            return false
        }

        val actualHash = calculateFileSha256Bytes(file)
        if (actualHash.isEmpty()) {
            appLogger.logError(Throwable("Validation failed: SHA256 calculation returned empty for ${file.absolutePath}"))
            return false
        }

        val expectedHash = asset.sha256
        val isValid = actualHash.contentEquals(expectedHash)
        if (!isValid) {
            appLogger.logError(
                Throwable(
                    "Validation failed: SHA256 mismatch for ${file.absolutePath}. " +
                            "expected=${expectedHash.toHexStringLower()} actual=${actualHash.toHexStringLower()}"
                )
            )
        }

        isValid
    }.getOrElse { e ->
        appLogger.logError(Throwable("Validation error for ${asset.localPath}: ${e.message}"))
        false
    }

    suspend fun calculateFileSha256Bytes(file: File): ByteArray = runCatching {
        val digest = MessageDigest.getInstance(AdConstants.SHA256_ALGORITHM)
        val buffer = ByteArray(64 * 1024)

        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }

        digest.digest()
    }.onFailure { e ->
        appLogger.logError(Throwable("Failed to calculate SHA256 for ${file.absolutePath}: ${e.message}"))
    }.getOrDefault(byteArrayOf())
}