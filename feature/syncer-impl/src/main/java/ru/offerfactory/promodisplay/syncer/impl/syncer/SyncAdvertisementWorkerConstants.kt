package ru.offerfactory.promodisplay.syncer.impl.syncer

object SyncAdvertisementWorkerConstants {
    const val PERIODIC_SYNC_WORK_NAME = "advertisement_config_sync_periodic"
    const val PERIODIC_SYNC_WORK_TAG = "advertisement_sync_tag"
    const val STARTUP_SYNC_WORK_NAME = "advertisement_config_sync_startup"
    const val WORKER_BACKOFF_TIME_SECONDS : Long = 30
    const val WORKER_MIN_DELAY_SECONDS : Long  = 900
}