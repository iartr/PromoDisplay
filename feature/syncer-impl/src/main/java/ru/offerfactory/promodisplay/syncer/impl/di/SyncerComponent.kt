package ru.offerfactory.promodisplay.syncer.impl.di

import android.content.Context
import androidx.work.WorkManager
import dagger.BindsInstance
import dagger.Component
import ru.offerfactory.promodisplay.network.di.NetworkModule
import ru.offerfactory.promodisplay.settings.di.SettingsModule
import ru.offerfactory.promodisplay.syncer.impl.syncer.SyncWorkerFactory
import syncer.AdvertisementSyncer
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        SyncerDataModule::class,
        NetworkModule::class,
        SettingsModule::class
    ]
)

interface SyncerComponent {

    fun advertisementSyncer(): AdvertisementSyncer
    fun syncWorkerFactory(): SyncWorkerFactory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context,
            @BindsInstance workManager: WorkManager
        ): SyncerComponent
    }
}