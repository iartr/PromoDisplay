package ru.offerfactory.promodisplay.ad.source.impl

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.offerfactory.promodisplay.ad.source.api.AdvertisementApi
import ru.offerfactory.promodisplay.ad.source.api.domain.models.AdClip
import ru.offerfactory.promodisplay.ad.source.impl.data.local.AdStorageManager
import ru.offerfactory.promodisplay.ad.source.impl.data.remote.AdFileDownloader
import ru.offerfactory.promodisplay.ad.source.impl.domain.models.*
import ru.offerfactory.promodisplay.ad.source.impl.util.AdConstants
import ru.offerfactory.promodisplay.ad.source.impl.util.AdFileValidator
import ru.offerfactory.promodisplay.ad.source.impl.util.hexToByteArray
import ru.offerfactory.promodisplay.logger.AppLogger
import ru.offerfactory.promodisplay.settings.ConfigManager
import ru.offerfactory.promodisplay.network.domain.util.NetworkConfig
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

    override suspend fun getClips(): Flow<List<AdClip>> = _internalItems
        .map { items ->
            items.filter { it.downloadState is DownloadState.Completed }
        }
        .distinctUntilChanged()
        .map { completedItems ->
            completedItems
                .sortedByDescending { it.priority }
                .flatMap { item ->
                    List(item.repeatInCycle) {
                        AdClip(
                            id = item.id,
                            videoUri = item.asset.localPath,
                            isReady = true
                        )
                    }
                }
        }

    private suspend fun handleConfigUpdate() {
        configManager.getConfig().collectLatest { config ->
            appLogger.logEvent("Received config: version ${config?.version}, items: ${config?.items?.size ?: 0}")
            if (config == null) {
                _internalItems.value = emptyList()
                return@collectLatest
            }
            val updatedList = config.items.map { remote ->
                val adFile = storage.getFileForId(remote.id)
                val isDone = adFile.exists() && adFile.length() == remote.asset.sizeBytes

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
                        localPath = adFile.absolutePath
                    ),
                    downloadState = if (isDone) DownloadState.Completed else DownloadState.Pending
                )
            }.sortedByDescending { it.priority }

            _internalItems.value = updatedList
            storage.cleanupOldFiles(config.items.map { it.id }.toSet())
            appLogger.logEvent("Cleaned up old files, kept ${config.items.size}")
            downloadInQueue(updatedList)
        }
    }

    private suspend fun downloadInQueue(items: List<AdItem>) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            val current = _internalItems.value.find { it.id == item.id } ?: return@forEach
            if (current.downloadState is DownloadState.Completed) return@forEach

            downloadSingleItem(current)
        }
    }

    private suspend fun downloadSingleItem(item: AdItem) {
        val path = item.asset.localPath
        if (path == null) {
            appLogger.logError(Throwable("No local path for ad ${item.id}"))
            updateItemState(item.id, DownloadState.Failed("No local path"))
            return
        }

        val file = File(path)

        if (adFileValidator.checkValidClips(item.asset)) {
            updateItemState(item.id, DownloadState.Completed)
            return
        }

        updateItemState(item.id, DownloadState.Downloading(0))

        val result = downloader.download(
            url = "${NetworkConfig.BASE_URL}${item.id}",
            file = file,
            expectedSize = item.asset.sizeBytes,
            onProgress = { progress ->
                appLogger.logEvent("Download progress for ${item.id}: $progress%")
                updateItemState(item.id, DownloadState.Downloading(progress.coerceIn(0, 100)))
            }
        )

        result.onSuccess {
            if (adFileValidator.checkValidClips(item.asset)) updateItemState(item.id, DownloadState.Completed)
            else {
                file.delete()
                appLogger.logError(Throwable("Hash validation failed for ad ${item.id}"))

                updateItemState(item.id, DownloadState.Failed("Hash fail"))
            }
        }.onFailure {
            appLogger.logError(Throwable("Download failed for ad ${item.id}"))
            updateItemState(item.id, DownloadState.Failed(it.message ?: "Error"))
        }
    }

    private fun updateItemState(id: String, state: DownloadState) {
        _internalItems.update { currentList ->
            currentList.map { item ->
                if (item.id == id) item.copy(downloadState = state) else item
            }
        }
    }
}