package ru.offerfactory.promodisplay.ad.source.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.offerfactory.promodisplay.ad.source.api.AdvertisementApi
import ru.offerfactory.promodisplay.ad.source.api.domain.models.AdClip
import ru.offerfactory.promodisplay.ad.source.impl.data.local.AdStorageManager
import ru.offerfactory.promodisplay.ad.source.impl.data.remote.AdFileDownloader
import ru.offerfactory.promodisplay.ad.source.impl.domain.models.AdAsset
import ru.offerfactory.promodisplay.ad.source.impl.domain.models.AdItem
import ru.offerfactory.promodisplay.ad.source.impl.domain.models.DownloadState
import ru.offerfactory.promodisplay.ad.source.impl.util.AdConstants
import ru.offerfactory.promodisplay.ad.source.impl.util.AdFileValidator
import ru.offerfactory.promodisplay.ad.source.impl.util.hexToByteArray
import ru.offerfactory.promodisplay.logger.AppLogger
import ru.offerfactory.promodisplay.network.domain.util.NetworkConfig
import ru.offerfactory.promodisplay.settings.ConfigManager
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdvertisementImpl @Inject constructor(
    private val configManager: ConfigManager,
    private val downloader: AdFileDownloader,
    private val storage: AdStorageManager,
    private val appLogger: AppLogger,
    private val adFileValidator: AdFileValidator,
    scope: CoroutineScope
) : AdvertisementApi {

    private val _internalItems = MutableStateFlow<List<AdItem>>(emptyList())

    init {
        scope.launch(Dispatchers.IO) { handleConfigUpdate() }
    }

    override fun getClips(): Flow<List<AdClip>> = _internalItems
        .map { items ->
            items
                .asSequence()
                .filter { it.downloadState is DownloadState.Completed }
                .flatMap { item ->
                    (0 until item.repeatInCycle).asSequence().map {
                        AdClip(
                            id = item.id,
                            videoUri = item.asset.localPath,
                            isReady = true
                        )
                    }
                }
                .toList()
        }
        .distinctUntilChanged()

    override fun getFirstClipDownloadProgress(): Flow<Int> = _internalItems
        .map { items ->
            val first = items.firstOrNull() ?: return@map 0
            when (val st = first.downloadState) {
                is DownloadState.Downloading -> st.progress.coerceIn(0, 100)
                is DownloadState.Completed -> 100
                is DownloadState.Pending -> 0
                is DownloadState.Failed -> 0
            }
        }
        .distinctUntilChanged()

    private suspend fun handleConfigUpdate() {
        configManager.getConfig().collectLatest { config ->
            appLogger.logEvent("Received config: version ${config?.version}, items: ${config?.items?.size ?: 0}")

            if (config == null) {
                _internalItems.value = emptyList()
                return@collectLatest
            }

            val updatedList = config.items
                .map { remote ->
                    val finalFile = storage.getFileForId(remote.id)

                    AdItem(
                        id = remote.id,
                        priority = remote.priority,
                        repeatInCycle = remote.repeatInCycle,
                        asset = AdAsset(
                            mimeType = AdConstants.MIME_TYPE_VIDEO,
                            sizeBytes = remote.asset.sizeBytes,
                            durationMs = remote.asset.durationMs,
                            sha256 = remote.asset.sha256.hexToByteArray(),
                            updatedAt = Instant.now(),
                            supportsRange = remote.asset.supportsRange,
                            localPath = finalFile.absolutePath
                        ),
                        downloadState = DownloadState.Pending
                    )
                }
                .sortedByDescending { it.priority }

            _internalItems.value = updatedList

            storage.cleanupOldFiles(config.items.map { it.id }.toSet())
            appLogger.logEvent("Cleaned up old files, kept ${config.items.size}")

            downloadInQueue(updatedList)
        }
    }

    private suspend fun downloadInQueue(items: List<AdItem>) = withContext(Dispatchers.IO) {
        for (item in items) {
            val current = _internalItems.value.find { it.id == item.id } ?: continue
            ensureDownloadedAndValid(current)
        }
    }

    private suspend fun ensureDownloadedAndValid(item: AdItem) {
        val finalPath = item.asset.localPath
        if (finalPath == null) {
            appLogger.logError(Throwable("No local path for ad ${item.id}"))
            updateItemState(item.id, DownloadState.Failed("No local path"))
            return
        }

        val finalFile = File(finalPath)

        if (finalFile.exists()) {
            val isValid = adFileValidator.checkValidClips(item.asset)
            if (isValid) {
                updateItemState(item.id, DownloadState.Completed)
                return
            } else {
                finalFile.delete()
            }
        }

        updateItemState(item.id, DownloadState.Downloading(0))

        val tempFile = File(finalFile.parentFile, "${finalFile.name}.tmp")
        val badFile = File(finalFile.parentFile, "${finalFile.nameWithoutExtension}.bad.${finalFile.extension}")

        if (tempFile.exists()) tempFile.delete()
        if (badFile.exists()) badFile.delete()

        val urls = buildMediaUrls(item.id)

        for ((index, url) in urls.withIndex()) {
            appLogger.logEvent("Ad ${item.id}: download try ${index + 1}/${urls.size} url=$url")

            val result = downloader.download(
                url = url,
                file = tempFile,
                expectedSize = item.asset.sizeBytes,
                onProgress = { progress ->
                    updateItemState(item.id, DownloadState.Downloading(progress.coerceIn(0, 100)))
                }
            )

            if (result.isFailure) {
                // downloader сам логирует детально (404/JSON wrapper/и т.д.)
                continue
            }

            val tempAsset = item.asset.copy(localPath = tempFile.absolutePath)
            val isValid = adFileValidator.checkValidClips(tempAsset)

            if (isValid) {
                if (finalFile.exists()) finalFile.delete()

                val renamed = tempFile.renameTo(finalFile)
                if (!renamed) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }

                appLogger.logEvent("Ad ${item.id}: downloaded + validated OK. saved to ${finalFile.absolutePath}")
                updateItemState(item.id, DownloadState.Completed)
                return
            } else {
                if (badFile.exists()) badFile.delete()
                tempFile.renameTo(badFile)
                appLogger.logError(Throwable("Ad ${item.id}: SHA mismatch. kept bad file at ${badFile.absolutePath}"))
            }
        }

        updateItemState(item.id, DownloadState.Failed("Hash/URL mismatch"))
    }

    /**
     * Реальность по логам:
     * - /media/{id} => 404
     * - /api-media/{id} => 200 application/json (wrapper)
     *
     * Значит начинаем с api-media.
     */
    private fun buildMediaUrls(assetId: String): List<String> {
        val base = NetworkConfig.BASE_URL.let { if (it.endsWith('/')) it else "$it/" }

        val apiMedia = "${base}api-media/$assetId"
        val media = "${base}media/$assetId"

        return listOf(apiMedia, media).distinct()
    }

    private fun updateItemState(id: String, state: DownloadState) {
        _internalItems.update { currentList ->
            currentList.map { item ->
                if (item.id == id) item.copy(downloadState = state) else item
            }
        }
    }
}