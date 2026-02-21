package ru.offerfactory.promodisplay.logger

interface AppLogger {

    fun logEvent(message : String)

    fun logError(throwable: Throwable)
}