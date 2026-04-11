package ru.offerfactory.promodisplay.di

import android.content.Context
import androidx.work.WorkManager
import dagger.BindsInstance
import dagger.Component
import ru.offerfactory.promodisplay.ad.source.api.AdvertisementApi
import ru.offerfactory.promodisplay.ad.source.impl.di.AdSourceModule
import ru.offerfactory.promodisplay.ad.source.impl.di.AdSourceRuntimeModule
import ru.offerfactory.promodisplay.logger.AppLogger
import ru.offerfactory.promodisplay.logger.LoggerModule
import ru.offerfactory.promodisplay.network.di.NetworkModule
import ru.offerfactory.promodisplay.player.api.PlayerApi
import ru.offerfactory.promodisplay.settings.di.SettingsModule
import ru.offerfactory.promodisplay.syncer.AdvertisementSyncer
import ru.offerfactory.promodisplay.syncer.impl.di.SyncerDataModule
import ru.offerfactory.promodisplay.syncer.impl.syncer.SyncWorkerFactory
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        LoggerModule::class,
        NetworkModule::class,
        SettingsModule::class,
        SyncerDataModule::class,
        AdSourceModule::class,
        AdSourceRuntimeModule::class,
        PlayerModule::class
    ]
)
interface AppComponent {

    fun workManager(): WorkManager

    fun appLogger(): AppLogger

    fun syncWorkerFactory(): SyncWorkerFactory

    fun advertisementSyncer(): AdvertisementSyncer

    fun advertisementApi(): AdvertisementApi

    fun playerApi(): PlayerApi

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}