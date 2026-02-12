package ru.offerfactory.promodisplay.syncer.impl.syncer

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository
import syncer.AdvertisementSyncer
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AdvertisementSyncerImpl @Inject constructor(
    private val repository: ConfigRepository,
    private val workManager: WorkManager
) : AdvertisementSyncer {

    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncAdvertisementWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            "advertisement_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override suspend fun syncNow() {
        repository.fetchConfig()
    }
}