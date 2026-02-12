package ru.offerfactory.promodisplay.syncer.impl.syncer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.offerfactory.promodisplay.network.domain.util.NetworkResult
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository
import ru.offerfactory.promodisplay.syncer.impl.domain.usecase.ApplyRemoteConfigUseCase

class SyncAdvertisementWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val repository: ConfigRepository,
    private val applyRemoteConfigUseCase: ApplyRemoteConfigUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return when (val result = repository.fetchConfig()) {
            is NetworkResult.Success -> {
                applyRemoteConfigUseCase.applyIfChanged(result.data)
                Result.success()
            }
            is NetworkResult.Failure -> Result.retry()
        }
    }
}
