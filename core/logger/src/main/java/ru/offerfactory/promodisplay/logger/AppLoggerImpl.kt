package ru.offerfactory.promodisplay.logger

import timber.log.Timber

class AppLoggerImpl : AppLogger {

    override fun logEvent(message: String) {
        Timber.d(message)
    }

    override fun logError(throwable: Throwable) {
        Timber.e(throwable)
    }
}