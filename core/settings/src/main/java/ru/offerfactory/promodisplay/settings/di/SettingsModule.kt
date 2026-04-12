package ru.offerfactory.promodisplay.settings.di

import dagger.Binds
import dagger.Module
import ru.offerfactory.promodisplay.settings.ConfigManager
import ru.offerfactory.promodisplay.settings.ConfigManagerImpl
import javax.inject.Singleton

@Module(
    includes = [SettingsDataStoreModule::class]
)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindConfigManager(configManagerImpl: ConfigManagerImpl): ConfigManager
}