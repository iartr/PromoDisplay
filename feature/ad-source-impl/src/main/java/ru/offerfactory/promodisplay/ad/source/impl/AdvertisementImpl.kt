package ru.offerfactory.promodisplay.ad.source.impl

import android.net.Network
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.offerfactory.promodisplay.ad.source.api.AdvertisementApi
import ru.offerfactory.promodisplay.ad.source.api.domain.models.AdClip
import ru.offerfactory.promodisplay.ad.source.impl.data.local.AdStorageManager
import ru.offerfactory.promodisplay.ad.source.impl.data.remote.AdFileDownloader
import ru.offerfactory.promodisplay.ad.source.impl.domain.models.*
import ru.offerfactory.promodisplay.ad.source.impl.util.AdConstants
import ru.offerfactory.promodisplay.ad.source.impl.util.AdFileValidator.checkValidClips
import ru.offerfactory.promodisplay.ad.source.impl.util.AdFileValidator.hexToByteArray
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
    private val network: Network
    scope: CoroutineScope
) : AdvertisementApi {



    private val _internalItems = MutableStateFlow<List<AdItem>>(emptyList())

    init {
        scope.launch(Dispatchers.IO) { handleConfigUpdate() }
    }

    override suspend fun getClips(): Flow<List<AdClip>> = _internalItems.map { items ->
        items.filter { it.downloadState is DownloadState.Completed }
            .sortedByDescending { it.priority }
            .flatMap { item ->
                List(item.repeatInCycle) {
                    AdClip(
                        item.id,
                        item.asset.localPath,
                        true
                    )
                }
            }
    }

    private suspend fun handleConfigUpdate() {
        configManager.getConfig().collectLatest { config ->
            val updatedList = config.items.map { remote ->
                val adFile = storage.getFileForId(remote.id)
                val isDone = adFile.exists() && adFile.length() == remote.sizeBytes

                AdItem(
                    id = remote.id,
                    priority = remote.priority,
                    repeatInCycle = remote.repeatInCycle,
                    asset = AdAsset(
                        AdConstants.MIME_TYPE_VIDEO,
                        remote.sizeBytes,
                        0L,
                        remote.sha256.hexToByteArray(),
                        Instant.now(),
                        true,
                        adFile.absolutePath
                    ),
                    downloadState = if (isDone) DownloadState.Completed else DownloadState.Pending
                )
            }.sortedByDescending { it.priority }

            _internalItems.value = updatedList
            storage.cleanupOldFiles(config.items.map { it.id }.toSet())
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
            updateItemState(item.id, DownloadState.Failed("No local path"))
            return
        }

        val file = File(path)

        if (checkValidClips(item.asset)) {
            updateItemState(item.id, DownloadState.Completed)
            return
        }

        updateItemState(item.id, DownloadState.Downloading(0))

        val result = downloader.download(
            url = "https://wlzywiojhnktpabjyvii.supabase.co/functions/v1/api-media/${item.id}",
            file = file,
            expectedSize = item.asset.sizeBytes,
            onProgress = { updateItemState(item.id, DownloadState.Downloading(it)) }
        )

        result.onSuccess {
            if (checkValidClips(item.asset)) updateItemState(item.id, DownloadState.Completed)
            else {
                file.delete(); updateItemState(item.id, DownloadState.Failed("Hash fail"))
            }
        }.onFailure {
            updateItemState(item.id, DownloadState.Failed(it.message ?: "Error"))
        }
    }

    private fun updateItemState(id: String, state: DownloadState) {
        _internalItems.value =
            _internalItems.value.map { if (it.id == id) it.copy(downloadState = state) else it }
    }
}