package ru.offerfactory.promodisplay.syncer.impl.di

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import ru.offerfactory.promodisplay.settings.ConfigManager
import ru.offerfactory.promodisplay.syncer.impl.data.mappers.ConfigMappers
import ru.offerfactory.promodisplay.syncer.impl.data.remote.ConfigApi
import ru.offerfactory.promodisplay.syncer.impl.data.repository.ConfigRepositoryImpl
import ru.offerfactory.promodisplay.syncer.impl.data.usecase.ApplyRemoteConfigUseCaseImpl
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository
import ru.offerfactory.promodisplay.syncer.impl.domain.usecase.ApplyRemoteConfigUseCase
import ru.offerfactory.promodisplay.syncer.impl.syncer.AdvertisementSyncerImpl
import syncer.AdvertisementSyncer
import javax.inject.Singleton

@Module
class SyncerDataModule {
    @Provides
    fun provideConfigApi(retrofit: Retrofit): ConfigApi =
        retrofit.create(ConfigApi::class.java)

    @Provides
    fun provideConfigMappers(): ConfigMappers = ConfigMappers()

    @Provides
    fun provideConfigRepository(
        api: ConfigApi,
        mappers: ConfigMappers
    ): ConfigRepository =
        ConfigRepositoryImpl(api, mappers)

    @Provides
    @Singleton
    fun provideAdvertisementSyncer(
        impl: AdvertisementSyncerImpl
    ): AdvertisementSyncer = impl

    @Provides
    @Singleton
    fun provideApplyRemoteConfigUseCase(
        configManager: ConfigManager
    ): ApplyRemoteConfigUseCase =
        ApplyRemoteConfigUseCaseImpl(configManager)
}