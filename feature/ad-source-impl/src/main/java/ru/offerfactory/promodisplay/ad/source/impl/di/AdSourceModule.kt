package ru.offerfactory.promodisplay.ad.source.impl.di

import dagger.Binds
import dagger.Module
import ru.offerfactory.promodisplay.ad.source.api.AdvertisementApi
import ru.offerfactory.promodisplay.ad.source.impl.AdvertisementImpl
import javax.inject.Singleton

@Module
abstract class AdSourceModule {
    @Binds
    @Singleton
    abstract fun bindAdSource(impl: AdvertisementImpl): AdvertisementApi
}