package ru.offerfactory.promodisplay.syncer.impl.syncer

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ru.offerfactory.promodisplay.syncer.AdvertisementSyncer
import ru.offerfactory.promodisplay.syncer.impl.syncer.SyncAdvertisementWorkerConstants.PERIODIC_SYNC_WORK_TAG
import ru.offerfactory.promodisplay.syncer.impl.syncer.SyncAdvertisementWorkerConstants.STARTUP_SYNC_WORK_NAME
import javax.inject.Inject

class AdvertisementSyncerImpl @Inject constructor(
    private val context: Context
) : AdvertisementSyncer {

    override fun schedulePeriodicSync() {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val configFetchOnStartup = OneTimeWorkRequestBuilder<SyncAdvertisementWorker>()
            .setConstraints(constraints)
            .addTag(PERIODIC_SYNC_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            STARTUP_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            configFetchOnStartup
        )
    }
}