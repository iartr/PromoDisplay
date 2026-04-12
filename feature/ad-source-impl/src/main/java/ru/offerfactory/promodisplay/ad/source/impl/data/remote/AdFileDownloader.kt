package ru.offerfactory.promodisplay.ad.source.impl.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import ru.offerfactory.promodisplay.ad.source.impl.util.AdConstants
import ru.offerfactory.promodisplay.logger.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class AdFileDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val appLogger: AppLogger
) {

    suspend fun download(
        apiMediaUrl: String,
        file: File,
        expectedSize: Long,
        supportsRange: Boolean,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val resolvedUrlResult = resolveStorageUrl(apiMediaUrl)
        if (resolvedUrlResult.isFailure) {
            return@withContext Result.failure(
                resolvedUrlResult.exceptionOrNull() ?: Exception("Failed to resolve storage url")
            )
        }

        val storageUrl = resolvedUrlResult.getOrThrow()
        appLogger.logEvent("AdFileDownloader: resolved storage url=$storageUrl")

        if (supportsRange) {
            downloadByHttpRange(
                url = storageUrl,
                file = file,
                expectedSize = expectedSize,
                onProgress = onProgress
            )
        } else {
            downloadWholeFile(
                url = storageUrl,
                file = file,
                expectedSize = expectedSize,
                onProgress = onProgress
            )
        }
    }

    private suspend fun resolveStorageUrl(apiMediaUrl: String): Result<String> {
        var lastError: Exception? = null

        for (attempt in 1..AdConstants.RESOLVE_MAX_ATTEMPTS) {
            val request = Request.Builder()
                .url(apiMediaUrl)
                .header("Accept", "application/json")
                .header("Accept-Encoding", "identity")
                .build()

            appLogger.logEvent("AdFileDownloader: resolve api-media url=$apiMediaUrl attempt=$attempt")

            try {
                val result = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { response ->
                        val code = response.code
                        val contentType = response.header("Content-Type").orEmpty()

                        appLogger.logEvent(
                            "AdFileDownloader: resolve response code=$code contentType=$contentType url=$apiMediaUrl"
                        )

                        if (!response.isSuccessful) {
                            val msg = "HTTP $code for $apiMediaUrl"
                            appLogger.logError(Throwable(msg))
                            Result.failure(Exception(msg))
                        } else {
                            val body = response.body
                            if (body == null) {
                                val msg = "Empty body for $apiMediaUrl"
                                appLogger.logError(Throwable(msg))
                                Result.failure(Exception(msg))
                            } else {
                                val jsonText = body.string()
                                val storageUrl = extractFirstHttpUrlFromJson(jsonText)

                                if (!storageUrl.isNullOrBlank()) {
                                    Result.success(storageUrl)
                                } else {
                                    val snippet = jsonText.take(JSON_SNIPPET_MAX_CHARS)
                                    val msg =
                                        "api-media response doesn't contain storage url. bodySnippet=$snippet"
                                    appLogger.logError(Throwable(msg))
                                    Result.failure(Exception(msg))
                                }
                            }
                        }
                    }
                }

                if (result.isSuccess) return result
                lastError = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")

            } catch (e: Exception) {
                appLogger.logError(
                    Throwable("Resolve storage url failed for $apiMediaUrl attempt=$attempt: ${e.message}")
                )
                lastError = e
            }

            if (attempt < AdConstants.RESOLVE_MAX_ATTEMPTS) {
                val delayMs = AdConstants.RETRY_BASE_DELAY_MS * attempt
                appLogger.logEvent(
                    "AdFileDownloader: resolve retry url=$apiMediaUrl nextAttempt=${attempt + 1} delayMs=$delayMs"
                )
                delay(delayMs)
            }
        }

        return Result.failure(lastError ?: Exception("Failed to resolve storage url"))
    }

    private suspend fun downloadByHttpRange(
        url: String,
        file: File,
        expectedSize: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (expectedSize <= 0L) {
            val msg = "Expected size must be > 0 for range download. url=$url expectedSize=$expectedSize"
            appLogger.logError(Throwable(msg))
            return@withContext Result.failure(Exception(msg))
        }

        file.parentFile?.mkdirs()

        if (file.exists() && file.length() > expectedSize) {
            appLogger.logEvent(
                "AdFileDownloader: partial file is larger than expected, deleting. " +
                        "file=${file.name} currentSize=${file.length()} expectedSize=$expectedSize"
            )
            file.delete()
        }

        onProgress(file.length(), expectedSize)

        while (file.length() < expectedSize) {
            val rangeStart = file.length()
            val rangeEnd = min(
                rangeStart + AdConstants.RANGE_CHUNK_SIZE_BYTES - 1L,
                expectedSize - 1L
            )

            var chunkCompleted = false
            var lastError: Exception? = null

            for (attempt in 1..AdConstants.RANGE_REQUEST_MAX_ATTEMPTS) {
                val fileSizeBeforeRequest = file.length()

                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .header("Range", "bytes=$rangeStart-$rangeEnd")
                    .build()

                appLogger.logEvent(
                    "AdFileDownloader: range request url=$url file=${file.name} " +
                            "rangeStart=$rangeStart rangeEnd=$rangeEnd attempt=$attempt"
                )

                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val code = response.code
                        val contentType = response.header("Content-Type").orEmpty()
                        val contentLength = response.header("Content-Length").orEmpty()
                        val contentRange = response.header("Content-Range").orEmpty()
                        val acceptRanges = response.header("Accept-Ranges").orEmpty()

                        appLogger.logEvent(
                            "AdFileDownloader: range response code=$code contentType=$contentType " +
                                    "contentLength=$contentLength contentRange=$contentRange " +
                                    "acceptRanges=$acceptRanges url=$url"
                        )

                        when (code) {
                            AdConstants.HTTP_RANGE_NOT_SATISFIABLE -> {
                                val localSize = file.length()
                                val msg =
                                    "HTTP 416 for $url with currentSize=$localSize expectedSize=$expectedSize"
                                appLogger.logError(Throwable(msg))
                                return@withContext Result.failure(Exception(msg))
                            }

                            !in RETRYABLE_HTTP_CODES -> {
                                val msg =
                                    "Non-retryable HTTP $code for range download url=$url attempt=$attempt"
                                appLogger.logError(Throwable(msg))
                                lastError = Exception(msg)
                                // не делаем return — даём retry-циклу решить
                            }

                            AdConstants.HTTP_PARTIAL_CONTENT -> {
                                val body = response.body
                                if (body == null) {
                                    val msg = "Empty body for $url"
                                    appLogger.logError(Throwable(msg))
                                    lastError = Exception(msg)
                                    return@use
                                }

                                val contentRangeInfo = parseContentRange(contentRange)
                                if (contentRangeInfo == null) {
                                    val msg = "Invalid Content-Range for $url: '$contentRange'"
                                    appLogger.logError(Throwable(msg))
                                    return@withContext Result.failure(Exception(msg))
                                }

                                if (contentRangeInfo.start != fileSizeBeforeRequest) {
                                    val msg =
                                        "Unexpected Content-Range start for $url: " +
                                                "expected=$fileSizeBeforeRequest actual=${contentRangeInfo.start}"
                                    appLogger.logError(Throwable(msg))
                                    return@withContext Result.failure(Exception(msg))
                                }

                                if (contentRangeInfo.total != expectedSize) {
                                    val msg =
                                        "Unexpected Content-Range total for $url: " +
                                                "expected=$expectedSize actual=${contentRangeInfo.total}"
                                    appLogger.logError(Throwable(msg))
                                    return@withContext Result.failure(Exception(msg))
                                }

                                val expectedBytesThisRequest =
                                    contentRangeInfo.end - contentRangeInfo.start + 1L
                                var bytesWrittenThisRequest = 0L

                                RandomAccessFile(file, "rw").use { raf ->
                                    raf.seek(fileSizeBeforeRequest)
                                    body.byteStream().use { input ->
                                        val buffer = ByteArray(AdConstants.BUFFER_SIZE_BYTES)
                                        while (true) {
                                            val read = input.read(buffer)
                                            if (read == -1) break
                                            raf.write(buffer, 0, read)
                                            bytesWrittenThisRequest += read
                                            onProgress(fileSizeBeforeRequest + bytesWrittenThisRequest, expectedSize)
                                        }
                                    }
                                }

                                if (bytesWrittenThisRequest != expectedBytesThisRequest) {
                                    appLogger.logEvent(
                                        "AdFileDownloader: range body ended early but partial bytes are saved. " +
                                                "expectedChunkBytes=$expectedBytesThisRequest " +
                                                "actualChunkBytes=$bytesWrittenThisRequest " +
                                                "currentSize=${file.length()}"
                                    )
                                }

                                chunkCompleted = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    val currentFileSize = file.length()
                    val advanced = currentFileSize > fileSizeBeforeRequest
                    lastError = e

                    if (advanced) {
                        onProgress(currentFileSize, expectedSize)
                        appLogger.logEvent(
                            "AdFileDownloader: range request failed after partial write, continuing from saved bytes. " +
                                    "url=$url currentSize=$currentFileSize error=${e.message}"
                        )
                        chunkCompleted = true
                    } else {
                        appLogger.logError(
                            Throwable("Range request failed for $url attempt=$attempt: ${e.message}")
                        )

                        if (attempt < AdConstants.RANGE_REQUEST_MAX_ATTEMPTS) {
                            val delayMs = AdConstants.RETRY_BASE_DELAY_MS * attempt
                            appLogger.logEvent(
                                "AdFileDownloader: range retry url=$url nextAttempt=${attempt + 1} delayMs=$delayMs"
                            )
                            delay(delayMs)
                        }
                    }
                }

                if (chunkCompleted) break
            }

            if (!chunkCompleted) {
                return@withContext Result.failure(
                    lastError ?: Exception("Range request failed for $url")
                )
            }

            onProgress(file.length(), expectedSize)
        }

        appLogger.logEvent(
            "AdFileDownloader: range download complete url=$url fileSize=${file.length()}"
        )
        Result.success(Unit)
    }

    private suspend fun downloadWholeFile(
        url: String,
        file: File,
        expectedSize: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Unit> {
        var lastError: Exception? = null

        for (attempt in 1..AdConstants.RESOLVE_MAX_ATTEMPTS) {
            file.parentFile?.mkdirs()
            if (file.exists()) file.delete()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity")
                .build()

            appLogger.logEvent(
                "AdFileDownloader: full download url=$url file=${file.name} attempt=$attempt"
            )

            try {
                val result = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { response ->
                        val code = response.code
                        val contentType = response.header("Content-Type").orEmpty()
                        val contentLength = response.header("Content-Length").orEmpty()

                        appLogger.logEvent(
                            "AdFileDownloader: full response code=$code contentType=$contentType " +
                                    "contentLength=$contentLength url=$url"
                        )

                        if (code != AdConstants.HTTP_OK) {
                            val msg =
                                "Expected HTTP 200 for full download, but got $code for $url"
                            appLogger.logError(Throwable(msg))
                            return@withContext Result.failure<Unit>(Exception(msg))
                        }

                        val body = response.body
                        if (body == null) {
                            val msg = "Empty body for $url"
                            appLogger.logError(Throwable(msg))
                            return@withContext Result.failure<Unit>(Exception(msg))
                        }

                        val totalBytes =
                            if (expectedSize > 0L) expectedSize else body.contentLength()
                        var downloadedBytes = 0L

                        FileOutputStream(file, false).use { output ->
                            body.byteStream().use { input ->
                                val buffer = ByteArray(AdConstants.BUFFER_SIZE_BYTES)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    downloadedBytes += read
                                    onProgress(downloadedBytes, totalBytes)
                                }
                            }
                        }

                        appLogger.logEvent(
                            "AdFileDownloader: full download complete url=$url " +
                                    "downloadedBytes=$downloadedBytes fileSize=${file.length()}"
                        )
                        Result.success(Unit)
                    }
                }

                if (result.isSuccess) return result
                lastError = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")

            } catch (e: Exception) {
                appLogger.logError(
                    Throwable("Full download failed for $url attempt=$attempt: ${e.message}")
                )
                lastError = e
            }

            if (attempt < AdConstants.RESOLVE_MAX_ATTEMPTS) {
                val delayMs = AdConstants.RETRY_BASE_DELAY_MS * attempt
                appLogger.logEvent(
                    "AdFileDownloader: full download retry url=$url nextAttempt=${attempt + 1} delayMs=$delayMs"
                )
                delay(delayMs)
            }
        }

        return Result.failure(lastError ?: Exception("Full download failed for $url"))
    }

    private fun extractFirstHttpUrlFromJson(jsonText: String): String? = runCatching {
        val trimmed = jsonText.trim()
        val value: Any = when {
            trimmed.startsWith("{") -> JSONObject(trimmed)
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> return null
        }

        findFirstHttpUrl(value)
    }.getOrNull()

    private fun findFirstHttpUrl(value: Any?): String? {
        return when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val found = findFirstHttpUrl(value.opt(key))
                    if (!found.isNullOrBlank()) return found
                }
                null
            }

            is JSONArray -> {
                for (i in 0 until value.length()) {
                    val found = findFirstHttpUrl(value.opt(i))
                    if (!found.isNullOrBlank()) return found
                }
                null
            }

            is String -> {
                val s = value.trim()
                if (s.startsWith("http://") || s.startsWith("https://")) s else null
            }

            else -> null
        }
    }

    private fun parseContentRange(header: String): ContentRangeInfo? {
        val match = CONTENT_RANGE_REGEX.matchEntire(header.trim()) ?: return null
        val start = match.groupValues[1].toLongOrNull() ?: return null
        val end = match.groupValues[2].toLongOrNull() ?: return null
        val total = match.groupValues[3].toLongOrNull() ?: return null

        if (start > end) return null

        return ContentRangeInfo(
            start = start,
            end = end,
            total = total
        )
    }

    private data class ContentRangeInfo(
        val start: Long,
        val end: Long,
        val total: Long
    )

    private companion object {
        private const val JSON_SNIPPET_MAX_CHARS = 300
        private val CONTENT_RANGE_REGEX = Regex("bytes (\\d+)-(\\d+)/(\\d+)")

        private val RETRYABLE_HTTP_CODES = setOf(
            AdConstants.HTTP_PARTIAL_CONTENT,
            500, 502, 503, 504
        )
    }
}