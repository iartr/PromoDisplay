package ru.offerfactory.promodisplay.syncer.impl.syncer

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import syncer.AdvertisementSyncer
import java.util.concurrent.TimeUnit

class AdvertisementSyncerImpl(
    private val context: Context
) : AdvertisementSyncer {

    override fun schedulePeriodicSync() {
        val request =
            PeriodicWorkRequestBuilder<SyncAdvertisementWorker>(
                30, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "advertisement_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    override suspend fun syncNow() {
        //repository.fetchConfig()
    }
}