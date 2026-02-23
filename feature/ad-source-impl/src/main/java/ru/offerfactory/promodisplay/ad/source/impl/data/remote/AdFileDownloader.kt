package ru.offerfactory.promodisplay.ad.source.impl.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.offerfactory.promodisplay.ad.source.impl.util.AdConstants
import ru.offerfactory.promodisplay.logger.AppLogger
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdFileDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val appLogger: AppLogger
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
                if (currentSize in AdConstants.MIN_RESUME_SIZE_BYTES until expectedSize) {
                    addHeader("Range", "bytes=$currentSize-")
                }
            }
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = "HTTP ${response.code} for $url"
                    appLogger.logError(Throwable(errorMsg))
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val body = response.body
                if (body == null) {
                    val errorMsgBody = "Empty body for $url"
                    appLogger.logError(Throwable(errorMsgBody))
                    return@withContext Result.failure(Exception(errorMsgBody))
                }

                val isAppend = response.code == AdConstants.HTTP_PARTIAL_CONTENT
                var downloaded = if (isAppend) currentSize else 0L

                FileOutputStream(file, isAppend).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(AdConstants.BUFFER_SIZE_BYTES)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(
                                ((downloaded * AdConstants.PERCENT_FACTOR) / expectedSize).toInt()
                                    .coerceAtMost(AdConstants.PROGRESS_MAX)
                            )
                        }
                    }
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            appLogger.logError(Throwable("Download failed for $url"))
            Result.failure(e)
        }
    }
}