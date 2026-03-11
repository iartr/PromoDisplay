package ru.offerfactory.promodisplay.logger

import ru.ok.tracer.crash.report.TracerCrashReport
import timber.log.Timber

class AppLoggerImpl : AppLogger {

    override fun logEvent(message: String) {
        Timber.d(message)
    }

    override fun logError(throwable: Throwable) {
        Timber.e(throwable)

        TracerCrashReport.report(throwable)
    }
}