package ru.offerfactory.promodisplay.di

import android.content.Context
import androidx.work.WorkManager
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])

interface AppComponent {
    fun workManager(): WorkManager

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}