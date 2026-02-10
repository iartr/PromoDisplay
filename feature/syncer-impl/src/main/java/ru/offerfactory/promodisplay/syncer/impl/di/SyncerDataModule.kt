package ru.offerfactory.promodisplay.syncer.impl.di

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import ru.offerfactory.promodisplay.syncer.impl.data.mappers.ConfigMappers
import ru.offerfactory.promodisplay.syncer.impl.data.remote.ConfigApi
import ru.offerfactory.promodisplay.syncer.impl.data.repository.ConfigRepositoryImpl
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository

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
}