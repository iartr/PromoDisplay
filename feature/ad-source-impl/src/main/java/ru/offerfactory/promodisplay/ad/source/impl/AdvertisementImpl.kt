package ru.offerfactory.promodisplay.ad.source.impl
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.offerfactory.promodisplay.ad.source.api.AdvertisementApi
import ru.offerfactory.promodisplay.ad.source.api.domain.models.AdClip
import ru.offerfactory.promodisplay.ad.source.impl.domain.models.AdAsset
import ru.offerfactory.promodisplay.ad.source.impl.domain.models.AdItem
import ru.offerfactory.promodisplay.ad.source.impl.domain.models.DownloadState
import ru.offerfactory.promodisplay.ad.source.impl.util.AdFileValidator.checkValidClips
import ru.offerfactory.promodisplay.settings.ConfigManager
import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity
import ru.offerfactory.promodisplay.settings.domain.model.ConfigModel
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.hexToByteArray

@Singleton
class AdvertisementImpl @Inject constructor(
    private val configManager: ConfigManager,
    private val configEntity: ConfigEntity,
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    scope: CoroutineScope
) : AdvertisementApi {

    private val _internalItems = MutableStateFlow<List<AdItem>>(emptyList())
    private var downloadSemaphore = Semaphore(3)

    init {
        scope.launch {
            handleConfigUpdate()
        }
    }

    override suspend fun getClips(): Flow<List<AdClip>> {
        return _internalItems.map { items ->
            items
                .filter { it.downloadState is DownloadState.Completed }
                .sortedByDescending { it.priority }
                .flatMap { item ->
                    List(item.repeatInCycle) {
                        AdClip(
                            id = item.id,
                            videoUri = item.asset.localPath.orEmpty(),
                            isReady = true
                        )
                    }
                }
        }
    }

    private suspend fun observeConfig(): Flow<ConfigModel> {
        return configManager.getConfig()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun handleConfigUpdate() {
        observeConfig().collectLatest { config ->
            val maxParallel = configEntity.downloadPolicy.maxParallelDownloads ?: 3
            downloadSemaphore = Semaphore(maxParallel)

            val updatedList = config.items.map { remote ->
                val adFile = File(context.filesDir, "ads/${remote.id}.mp4")
                adFile.parentFile?.mkdirs()

                val initialState = if (adFile.exists() && adFile.length() == remote.sizeBytes) {
                    DownloadState.Completed
                } else {
                    DownloadState.Pending
                }

                AdItem(
                    id = remote.id,
                    priority = remote.priority,
                    repeatInCycle = remote.repeatInCycle,
                    downloadState = initialState,
                    asset = AdAsset(
                        mimeType = "video/mp4",
                        sizeBytes = remote.sizeBytes,
                        durationMs = 0L,
                        sha256 = remote.sha256.hexToByteArray(),
                        updatedAt = Instant.now(),
                        supportsRange = true,
                        localPath = adFile.absolutePath
                    ),
                )
            }.sortedByDescending { it.priority }

            _internalItems.value = updatedList

            downloadClips(updatedList)
        }
    }

    private suspend fun downloadClips(items: List<AdItem>) = withContext(Dispatchers.IO) {
        supervisorScope {
            items
                .filter {
                    it.downloadState !is DownloadState.Completed &&
                            it.downloadState !is DownloadState.Downloading
                }
                .forEach { item ->
                    launch {
                        downloadSemaphore.withPermit {
                            downloadSingleItem(item)
                        }
                    }
                }
        }
    }

    private suspend fun downloadSingleItem(item: AdItem) {
        val currentItem = _internalItems.value.find { it.id == item.id } ?: return
        if (currentItem.downloadState is DownloadState.Completed ||
            currentItem.downloadState is DownloadState.Downloading
        ) {
            return
        }
        updateItemState(item.id, DownloadState.Downloading(0))

        val asset = item.asset
        val adFile = File(asset.localPath ?: return)

        if (adFile.exists() && adFile.length() == asset.sizeBytes) {
            if (checkValidClips(asset)) {
                updateItemState(item.id, DownloadState.Completed)
                return
            } else {
                adFile.delete()
            }
        }

        val currentSize = if (adFile.exists()) adFile.length() else 0L

        val request = Request.Builder()
            .url("https://wlzywiojhnktpabjyvii.supabase.co/functions/v1/media/${item.id}")
            .apply {
                if (currentSize > 0 && asset.supportsRange) {
                    addHeader("Range", "bytes=$currentSize-")
                }
            }
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    updateItemState(item.id, DownloadState.Failed("Error: ${response.code}"))
                    return
                }

                val body = response.body ?: return
                val totalBytes = asset.sizeBytes
                var downloadedBytes = if (response.code == 206) currentSize else 0L
                var lastReportedProgress = -1

                FileOutputStream(adFile, response.code == 206).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val progress = ((downloadedBytes * 100 / totalBytes).toInt())
                            if (progress != lastReportedProgress) {
                                updateItemState(item.id, DownloadState.Downloading(progress))
                                lastReportedProgress = progress
                            }
                        }
                    }
                }

                if (checkValidClips(asset)) {
                    updateItemState(item.id, DownloadState.Completed)
                } else {
                    updateItemState(item.id, DownloadState.Failed("SHA256 mismatch"))
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            updateItemState(item.id, DownloadState.Failed(e.message ?: "Unknown error"))
        }
    }

    private fun updateItemState(id: String, state: DownloadState) {
        _internalItems.value = _internalItems.value.map {
            if (it.id == id) it.copy(downloadState = state) else it
        }
    }


}
