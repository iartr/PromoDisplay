package ru.offerfactory.promodisplay.syncer.impl.syncer

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.offerfactory.promodisplay.syncer.AdvertisementSyncer
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AdvertisementSyncerImpl @Inject constructor(
    private val context: Context
) : AdvertisementSyncer {

    override fun schedulePeriodicSync() {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // One-time синк на старте — делаем unique, чтобы не плодить воркеры.
        val configFetchOnStartup = OneTimeWorkRequestBuilder<SyncAdvertisementWorker>()
            .setConstraints(constraints)
            .build()

        val periodicConfigFetch = PeriodicWorkRequestBuilder<SyncAdvertisementWorker>(
            30,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            ADVERTISEMENT_CONFIG_SYNC_STARTUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            configFetchOnStartup
        )

        workManager.enqueueUniquePeriodicWork(
            ADVERTISEMENT_CONFIG_SYNC_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicConfigFetch
        )
    }

    private companion object {
        const val ADVERTISEMENT_CONFIG_SYNC_STARTUP_WORK_NAME = "advertisement_config_sync_startup"
        const val ADVERTISEMENT_CONFIG_SYNC_PERIODIC_WORK_NAME = "advertisement_config_sync_periodic"
    }
}