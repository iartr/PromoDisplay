package ru.offerfactory.promodisplay.syncer.impl.syncer

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository
import ru.offerfactory.promodisplay.syncer.impl.domain.usecase.ApplyRemoteConfigUseCase

class SyncWorkerFactory(
    private val repository: ConfigRepository,
    private val applyRemoteConfigUseCase: ApplyRemoteConfigUseCase
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = when (workerClassName) {
        SyncAdvertisementWorker::class.java.name -> SyncAdvertisementWorker(
            appContext,
            workerParameters,
            repository,
            applyRemoteConfigUseCase
        )
        else -> null
    }
}