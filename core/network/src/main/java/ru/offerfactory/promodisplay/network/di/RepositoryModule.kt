package ru.offerfactory.promodisplay.network.di

import dagger.Module
import dagger.Binds
import ru.offerfactory.promodisplay.network.data.repository.ConfigRepositoryImpl
import ru.offerfactory.promodisplay.network.domain.repository.ConfigRepository

@Module
abstract class RepositoryModule {

    @Binds
    abstract fun bindConfigRepository(
        impl: ConfigRepositoryImpl
    ): ConfigRepository
}
