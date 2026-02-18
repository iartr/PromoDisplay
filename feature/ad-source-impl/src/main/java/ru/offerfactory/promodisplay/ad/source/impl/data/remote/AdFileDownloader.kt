package ru.offerfactory.promodisplay.ad.source.impl.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdFileDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun download(
        url: String,
        file: File,
        expectedSize: Long,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val currentSize = if (file.exists()) file.length() else 0L

        val request = Request.Builder()
            .url(url)
            .apply {
                if (currentSize in 1 until expectedSize) {
                    addHeader("Range", "bytes=$currentSize-")
                }
            }
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))

                val body = response.body ?: return@withContext Result.failure(Exception("Empty body"))
                val isAppend = response.code == 206
                var downloaded = if (isAppend) currentSize else 0L

                FileOutputStream(file, isAppend).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(((downloaded * 100) / expectedSize).toInt().coerceAtMost(100))
                        }
                    }
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}