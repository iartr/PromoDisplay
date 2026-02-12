package ru.offerfactory.promodisplay.syncer.impl.di

import android.content.Context
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import ru.offerfactory.promodisplay.settings.ConfigManager
import ru.offerfactory.promodisplay.syncer.AdvertisementSyncer
import ru.offerfactory.promodisplay.syncer.impl.data.mappers.ConfigMappers
import ru.offerfactory.promodisplay.syncer.impl.data.remote.ConfigApi
import ru.offerfactory.promodisplay.syncer.impl.data.repository.ConfigRepositoryImpl
import ru.offerfactory.promodisplay.syncer.impl.domain.usecase.ApplyRemoteConfigUseCaseImpl
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository
import ru.offerfactory.promodisplay.syncer.impl.domain.usecase.ApplyRemoteConfigUseCase
import ru.offerfactory.promodisplay.syncer.impl.syncer.AdvertisementSyncerImpl
import ru.offerfactory.promodisplay.syncer.impl.syncer.SyncWorkerFactory
import javax.inject.Singleton

@Module
class SyncerDataModule {
    @Provides
    @Singleton
    fun provideConfigApi(retrofit: Retrofit): ConfigApi =
        retrofit.create(ConfigApi::class.java)

    @Provides
    @Singleton
    fun provideConfigMappers(): ConfigMappers = ConfigMappers()

    @Provides
    @Singleton
    fun provideConfigRepository(
        api: ConfigApi,
        mappers: ConfigMappers
    ): ConfigRepository =
        ConfigRepositoryImpl(api, mappers)

    @Provides
    @Singleton
    fun provideAdvertisementSyncer(
        context: Context
    ): AdvertisementSyncer = AdvertisementSyncerImpl(context)

    @Provides
    @Singleton
    fun provideApplyRemoteConfigUseCase(
        configManager: ConfigManager
    ): ApplyRemoteConfigUseCase =
        ApplyRemoteConfigUseCaseImpl(configManager)

    @Provides
    @Singleton
    fun provideSyncWorkerFactory(
        repository: ConfigRepository,
        applyRemoteConfigUseCase: ApplyRemoteConfigUseCase
    ): SyncWorkerFactory = SyncWorkerFactory(repository, applyRemoteConfigUseCase)
}