package ru.offerfactory.promodisplay.di

import android.content.Context
import androidx.work.WorkManager
import dagger.BindsInstance
import dagger.Component
import ru.offerfactory.promodisplay.logger.AppLogger
import ru.offerfactory.promodisplay.logger.LoggerModule
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, LoggerModule::class])

interface AppComponent {
    fun workManager(): WorkManager
    fun logger() : AppLogger

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}