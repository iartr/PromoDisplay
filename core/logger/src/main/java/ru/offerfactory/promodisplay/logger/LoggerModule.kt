package ru.offerfactory.promodisplay.logger

import android.app.Application
import dagger.Module
import dagger.Provides
import timber.log.Timber
import javax.inject.Singleton

@Module
object LoggerModule {

    @Provides
    @Singleton
    fun provideLogger(
        @IsDebugLogging isDebug : Boolean,
        application: Application
    ) : AppLogger{

        if(isDebug){
            Timber.plant(Timber.DebugTree())
        } else {
            //Timber.plant(FileLoggingTree(application))
        }

        return AppLoggerImpl()
    }
}