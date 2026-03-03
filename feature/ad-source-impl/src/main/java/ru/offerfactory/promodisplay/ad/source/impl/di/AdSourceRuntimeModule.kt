package ru.offerfactory.promodisplay.ad.source.impl.di

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
class AdSourceRuntimeModule {

    @Provides
    @Singleton
    fun provideAdSourceCoroutineScope(): CoroutineScope {
        // app-lifetime scope для ad-source: скачивание/валидирование/обновление очереди
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}