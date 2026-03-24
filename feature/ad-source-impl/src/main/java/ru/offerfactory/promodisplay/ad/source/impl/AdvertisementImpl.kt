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
            when (val state = first.downloadState) {
                is DownloadState.Downloading -> state.progress.coerceIn(0, 100)
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
                    val sizeOk = finalFile.exists() && finalFile.length() == remote.asset.sizeBytes
                    val initialState = if (sizeOk) DownloadState.Completed else DownloadState.Pending

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
                        downloadState = initialState
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
        val tempFile = storage.getTempFileForId(item.id)
        val badFile = storage.getBadFileForId(item.id)

        if (finalFile.exists()) {
            val isValid = adFileValidator.checkValidClips(item.asset)
            if (isValid) {
                updateItemState(item.id, DownloadState.Completed)
                return
            }

            finalFile.delete()
        }

        if (tempFile.exists() && tempFile.length() == item.asset.sizeBytes) {
            val tempAsset = item.asset.copy(localPath = tempFile.absolutePath)
            val isTempValid = adFileValidator.checkValidClips(tempAsset)
            if (isTempValid) {
                promoteTempFile(tempFile = tempFile, finalFile = finalFile)
                updateItemState(item.id, DownloadState.Completed)
                return
            }

            if (badFile.exists()) badFile.delete()
            tempFile.renameTo(badFile)
        }

        if (!item.asset.supportsRange && tempFile.exists()) {
            tempFile.delete()
        }

        if (tempFile.exists() && tempFile.length() > item.asset.sizeBytes) {
            tempFile.delete()
        }

        if (badFile.exists()) {
            badFile.delete()
        }

        val existingTempBytes = tempFile.takeIf { it.exists() }?.length() ?: 0L
        val initialProgress = if (item.asset.sizeBytes > 0L) {
            ((existingTempBytes * 100) / item.asset.sizeBytes).toInt().coerceIn(0, 100)
        } else {
            0
        }
        updateItemState(item.id, DownloadState.Downloading(initialProgress))

        val apiMediaUrl = buildApiMediaUrl(item.id)
        appLogger.logEvent("Ad ${item.id}: start api-media resolve url=$apiMediaUrl")

        val result = downloader.download(
            apiMediaUrl = apiMediaUrl,
            file = tempFile,
            expectedSize = item.asset.sizeBytes,
            supportsRange = item.asset.supportsRange,
            onProgress = { downloadedBytes, totalBytes ->
                val progress = if (totalBytes > 0L) {
                    ((downloadedBytes * 100) / totalBytes).toInt()
                } else {
                    0
                }

                updateItemState(
                    item.id,
                    DownloadState.Downloading(progress.coerceIn(0, 100))
                )
            }
        )

        if (result.isFailure) {
            val error = result.exceptionOrNull()?.message ?: "Download failed"
            appLogger.logError(Throwable("Ad ${item.id}: download failed: $error"))
            updateItemState(item.id, DownloadState.Failed(error))
            return
        }

        val tempAsset = item.asset.copy(localPath = tempFile.absolutePath)
        val isValid = adFileValidator.checkValidClips(tempAsset)

        if (isValid) {
            promoteTempFile(tempFile = tempFile, finalFile = finalFile)
            appLogger.logEvent("Ad ${item.id}: downloaded + validated OK. saved to ${finalFile.absolutePath}")
            updateItemState(item.id, DownloadState.Completed)
            return
        }

        if (badFile.exists()) badFile.delete()
        tempFile.renameTo(badFile)
        appLogger.logError(Throwable("Ad ${item.id}: SHA mismatch. kept bad file at ${badFile.absolutePath}"))
        updateItemState(item.id, DownloadState.Failed("Hash mismatch"))
    }

    private fun buildApiMediaUrl(assetId: String): String {
        val base = NetworkConfig.BASE_URL.let { if (it.endsWith('/')) it else "$it/" }
        return "${base}api-media/$assetId"
    }

    private fun promoteTempFile(tempFile: File, finalFile: File) {
        if (finalFile.exists()) finalFile.delete()

        val renamed = tempFile.renameTo(finalFile)
        if (!renamed) {
            tempFile.copyTo(finalFile, overwrite = true)
            tempFile.delete()
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