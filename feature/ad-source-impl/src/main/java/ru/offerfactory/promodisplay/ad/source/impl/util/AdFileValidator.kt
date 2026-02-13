package ru.offerfactory.promodisplay.ad.source.impl.util

import ru.offerfactory.promodisplay.ad.source.impl.domain.models.AdAsset
import java.io.File
import java.security.MessageDigest

object AdFileValidator {
    fun checkValidClips(asset: AdAsset): Boolean {
        val path = asset.localPath ?: return false
        val file = File(path)

        if (!file.exists() || file.length() != asset.sizeBytes) {
            return false
        }

        val fileHash = calculateFileSha256Bytes(file)

        return fileHash.contentEquals(asset.sha256)
    }

    fun calculateFileSha256Bytes(file: File): ByteArray {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)

            file.inputStream().use { inputStream ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest()
        } catch (e: Exception) {
            byteArrayOf()
        }

    }
}