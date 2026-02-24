package ru.offerfactory.promodisplay.settings.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import ru.offerfactory.promodisplay.settings.ConfigManager
import ru.offerfactory.promodisplay.settings.ConfigManagerImpl
import androidx.datastore.preferences.core.Preferences
import javax.inject.Singleton


val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "promodisplay_settings"
)

@Module
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindConfigManager(configManagerImpl: ConfigManagerImpl): ConfigManager

    companion object {
        @Provides
        @Singleton
        @JvmStatic
        fun provideSettingsDataStore(
            context: Context
        ): DataStore<Preferences> = context.settingsDataStore
    }
}