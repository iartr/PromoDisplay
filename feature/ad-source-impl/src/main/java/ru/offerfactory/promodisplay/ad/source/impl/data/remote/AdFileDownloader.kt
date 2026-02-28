package ru.offerfactory.promodisplay.ad.source.impl.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
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
        // 1) Пробуем скачать “как есть”
        val direct = downloadDirect(url, file, expectedSize, onProgress)

        // Если успех — ок
        if (direct.isSuccess) return@withContext direct

        // Если не успех — возможно, это JSON-wrapper. Попробуем отдельной попыткой “wrapper download”
        // Но нам нужно понимать: direct мог упасть по 404/500 и т.п. Тогда wrapper не поможет.
        // Поэтому вторую ветку делаем только если direct вернул специальную ошибку JSON_WRAPPER.
        val error = direct.exceptionOrNull()
        if (error is JsonWrapperException) {
            val resolvedUrl = error.resolvedUrl
            appLogger.logEvent("AdFileDownloader: resolved json wrapper url=$url -> $resolvedUrl")
            return@withContext downloadDirect(resolvedUrl, file, expectedSize, onProgress)
        }

        direct
    }

    /**
     * Скачивание “по URL”:
     * - Если Content-Type = application/json, то это НЕ mp4, а wrapper.
     *   Тогда бросаем JsonWrapperException с извлечённым URL.
     */
    private suspend fun downloadDirect(
        url: String,
        file: File,
        expectedSize: Long,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val currentSize = if (file.exists()) file.length() else 0L

        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")

        if (currentSize in AdConstants.MIN_RESUME_SIZE_BYTES until expectedSize) {
            requestBuilder.addHeader("Range", "bytes=$currentSize-")
        }

        val request = requestBuilder.build()

        appLogger.logEvent(
            "AdFileDownloader: start url=$url file=${file.name} currentSize=$currentSize expectedSize=$expectedSize"
        )

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                val contentType = response.header("Content-Type").orEmpty()
                val contentLength = response.header("Content-Length").orEmpty()

                appLogger.logEvent("AdFileDownloader: response code=$code contentType=$contentType contentLength=$contentLength url=$url")

                if (!response.isSuccessful) {
                    val msg = "HTTP $code for $url"
                    appLogger.logError(Throwable(msg))
                    return@withContext Result.failure(Exception(msg))
                }

                val body = response.body ?: run {
                    val msg = "Empty body for $url"
                    appLogger.logError(Throwable(msg))
                    return@withContext Result.failure(Exception(msg))
                }

                // КЛЮЧЕВОЕ: сервер отдаёт JSON (wrapper), а не видео
                if (contentType.lowercase().contains("application/json")) {
                    val jsonText = body.string()
                    val snippet = jsonText.take(300)
                    appLogger.logEvent("AdFileDownloader: json wrapper for url=$url bodySnippet=$snippet")

                    val extractedUrl = extractFirstHttpUrlFromJson(jsonText)
                    if (extractedUrl.isNullOrBlank()) {
                        val msg = "JSON wrapper didn't contain downloadable url. url=$url bodySnippet=$snippet"
                        appLogger.logError(Throwable(msg))
                        return@withContext Result.failure(Exception(msg))
                    }

                    // НЕ пишем json в файл. Сигналим верхнему уровню, что нужно скачать по extractedUrl
                    return@withContext Result.failure(JsonWrapperException(resolvedUrl = extractedUrl))
                }

                // Обычная ветка: пришли байты (видео или другой бинарник)
                val isAppend = code == AdConstants.HTTP_PARTIAL_CONTENT
                var downloaded = if (isAppend) currentSize else 0L

                FileOutputStream(file, isAppend).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(AdConstants.BUFFER_SIZE_BYTES)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(
                                ((downloaded * AdConstants.PERCENT_FACTOR) / expectedSize)
                                    .toInt()
                                    .coerceAtMost(AdConstants.PROGRESS_MAX)
                            )
                        }
                    }
                }

                appLogger.logEvent("AdFileDownloader: done url=$url downloadedBytes=$downloaded fileSize=${file.length()}")
                Result.success(Unit)
            }
        } catch (e: JsonWrapperException) {
            // Не логируем как “ошибка”: это ожидаемый сценарий для api-media
            Result.failure(e)
        } catch (e: Exception) {
            appLogger.logError(Throwable("Download failed for $url: ${e.message}"))
            Result.failure(e)
        }
    }

    /**
     * Пытаемся вытащить из JSON первую строку с http(s)://...
     * Поддерживаем вложенные объекты/массивы.
     */
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
                    val child = value.opt(key)
                    val found = findFirstHttpUrl(child)
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

    private class JsonWrapperException(val resolvedUrl: String) : RuntimeException("JSON_WRAPPER")
}