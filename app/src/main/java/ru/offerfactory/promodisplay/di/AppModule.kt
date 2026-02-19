package ru.offerfactory.promodisplay.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import ru.offerfactory.promodisplay.BuildConfig
import ru.offerfactory.promodisplay.logger.IsDebugLogging
import javax.inject.Singleton


@Module
object AppModule {

    @Provides
    @Singleton
    fun provideWorkManager(context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @IsDebugLogging
    fun provideIsDebugLogging() : Boolean{
        return BuildConfig.DEBUG
    }
}