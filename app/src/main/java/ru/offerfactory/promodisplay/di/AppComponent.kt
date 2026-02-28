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

        // чтобы ad-source и syncer получили OkHttp/Retrofit/Json
        NetworkModule::class,

        // чтобы syncer/ad-source слушали и обновляли конфиг
        SettingsModule::class,

        // чтобы работал WorkManager WorkerFactory + scheduling синка
        SyncerDataModule::class,

        // чтобы AdvertisementApi реально создался и отдавал клипы
        AdSourceModule::class,
        AdSourceRuntimeModule::class,

        // чтобы PlayerApi получался из DI
        PlayerModule::class
    ]
)
interface AppComponent {

    fun workManager(): WorkManager
    fun logger(): AppLogger

    // WorkManager
    fun syncWorkerFactory(): SyncWorkerFactory
    fun advertisementSyncer(): AdvertisementSyncer

    // Data/Player
    fun advertisementApi(): AdvertisementApi
    fun playerApi(): PlayerApi

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}