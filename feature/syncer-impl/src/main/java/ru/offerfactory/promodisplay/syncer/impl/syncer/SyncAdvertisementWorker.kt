package ru.offerfactory.promodisplay.syncer.impl.syncer

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.offerfactory.promodisplay.network.domain.util.NetworkResult
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository
import ru.offerfactory.promodisplay.syncer.impl.domain.usecase.ApplyRemoteConfigUseCase
import ru.offerfactory.promodisplay.syncer.impl.syncer.SyncAdvertisementWorkerConstants.PERIODIC_SYNC_WORK_NAME
import ru.offerfactory.promodisplay.syncer.impl.syncer.SyncAdvertisementWorkerConstants.PERIODIC_SYNC_WORK_TAG
import ru.offerfactory.promodisplay.syncer.impl.syncer.SyncAdvertisementWorkerConstants.WORKER_BACKOFF_TIME_SECONDS
import ru.offerfactory.promodisplay.syncer.impl.syncer.SyncAdvertisementWorkerConstants.WORKER_MIN_DELAY_SECONDS
import java.util.concurrent.TimeUnit
import kotlin.math.max

class SyncAdvertisementWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val repository: ConfigRepository,
    private val applyRemoteConfigUseCase: ApplyRemoteConfigUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        return@withContext when (val result = repository.fetchConfig()) {
            is NetworkResult.Success -> {
                val config = result.data
                applyRemoteConfigUseCase.applyIfChanged(config)
                scheduleNextSync(config.pollIntervalSec)
                Result.success()
            }

            is NetworkResult.Failure -> Result.retry()
        }
    }

    private fun scheduleNextSync(delaySeconds: Long) {
        val cooldown = max(delaySeconds, WORKER_MIN_DELAY_SECONDS)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val nextWork =
            OneTimeWorkRequestBuilder<SyncAdvertisementWorker>()
                .setInitialDelay(cooldown, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WORKER_BACKOFF_TIME_SECONDS,
                    TimeUnit.SECONDS
                )
                .addTag(PERIODIC_SYNC_WORK_TAG)
                .build()

        WorkManager
            .getInstance(applicationContext)
            .enqueueUniqueWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                nextWork
            )
    }
}