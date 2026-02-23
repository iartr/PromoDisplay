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

class AdFileValidator @Inject constructor(
    private val appLogger: AppLogger
) {
    suspend fun checkValidClips(asset: AdAsset): Boolean = runCatching {
        val path = asset.localPath ?: return false.also {
            appLogger.logError(Throwable("No local path in asset for validation"))
        }

        val file = File(path).also { f ->
            if (!f.exists()) {
                appLogger.logError(Throwable("File not found for validation: ${f.absolutePath}"))
                return false
            }
            if (f.length() != asset.sizeBytes) {
                appLogger.logError(Throwable("Size mismatch for ${f.absolutePath}: expected ${asset.sizeBytes}, actual ${f.length()}"))
                return false
            }
        }

        val fileHash = calculateFileSha256Bytes(file)
        if (fileHash.isEmpty()) {
            appLogger.logError(Throwable("Failed to calculate SHA256 for ${file.absolutePath}"))
            return false
        }

        val isValid = fileHash.contentEquals(asset.sha256)
        if (!isValid) {
            appLogger.logError(Throwable("SHA256 mismatch for ${file.absolutePath}"))
        }
        isValid
    }.getOrElse {
        appLogger.logError(Throwable("Unexpected error in checkValidClips for ${asset.localPath}"))
        false
    }

    suspend fun calculateFileSha256Bytes(file: File): ByteArray = runCatching {
        val digest = MessageDigest.getInstance(AdConstants.SHA256_ALGORITHM)
        val buffer = ByteArray(64 * 1024)

        file.inputStream().use { input ->
            generateSequence { input.read(buffer) }
                .takeWhile { it != -1 }
                .forEach { digest.update(buffer, 0, it) }
        }
        digest.digest()
    }.onFailure { e ->
        appLogger.logError(Throwable("Failed to calculate SHA256 for ${file.absolutePath}"))
    }.getOrDefault(byteArrayOf())
}