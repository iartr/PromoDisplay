package ru.offerfactory.promodisplay.syncer.impl.mock

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import ru.offerfactory.promodisplay.network.domain.util.NetworkResult
import ru.offerfactory.promodisplay.settings.ConfigManager
import ru.offerfactory.promodisplay.syncer.impl.domain.model.ConfigInfo
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository

class SyncAdvertisementWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val repository: ConfigRepository,
    private val configManager: ConfigManager
) : Worker(context, workerParams) {

    override fun doWork(): Result = runBlocking {
        try {
            when (val networkResult: NetworkResult<ConfigInfo> = repository.fetchConfig()) {
                is NetworkResult.Success -> {
                    val newConfig = networkResult.data

                    val currentConfig = configManager.getConfig().firstOrNull()

                    if (currentConfig?.items != newConfig.items) {
                        //configManager.saveConfig(newConfig)
                        TODO("mapper ConfigInfo -> ConfigModel?")
                    }

                    Result.success()
                }

                is NetworkResult.Failure -> {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
