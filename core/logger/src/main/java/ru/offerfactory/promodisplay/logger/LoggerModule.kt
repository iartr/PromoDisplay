package ru.offerfactory.promodisplay.logger

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import timber.log.Timber
import javax.inject.Singleton

@Module
object LoggerModule {

    @Provides
    @Singleton
    fun provideAppLogger(
        @IsDebugLogging isDebug: Boolean,
        application: Application
    ): AppLogger {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(FileLoggingTree(context = application))
        }

        return AppLoggerImpl()
    }

    @Provides
    @Singleton
    fun provideApplication(context: Context): Application {
        return context.applicationContext as Application
    }
}