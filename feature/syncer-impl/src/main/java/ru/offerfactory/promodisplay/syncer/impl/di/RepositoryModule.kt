package ru.offerfactory.promodisplay.syncer.impl.di

import dagger.Binds
import dagger.Module
import ru.offerfactory.promodisplay.syncer.impl.data.repository.ConfigRepositoryImpl
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository

@Module
abstract class RepositoryModule {

    @Binds
    abstract fun bindConfigRepository(
        impl: ConfigRepositoryImpl
    ): ConfigRepository
}