package ru.offerfactory.promodisplay.syncer.impl.syncer

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.offerfactory.promodisplay.syncer.AdvertisementSyncer
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AdvertisementSyncerImpl @Inject constructor(
    private val workManager: WorkManager
) : AdvertisementSyncer {

    override fun schedulePeriodicSync() {
        val configFetchOnStartup = OneTimeWorkRequestBuilder<SyncAdvertisementWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        val periodicalConfigFetch = PeriodicWorkRequestBuilder<SyncAdvertisementWorker>(
            30,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()

        workManager.enqueue(configFetchOnStartup)
        workManager.enqueueUniquePeriodicWork(
            ADVERTISEMENT_CONFIG_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicalConfigFetch
        )
    }

    private companion object {
        const val ADVERTISEMENT_CONFIG_SYNC_WORK_NAME = "advertisement_config_sync"
    }
}