package ru.offerfactory.promodisplay.settings.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

private const val PREFERENCES_DATA_STORE_FILE_NAME = "promo_display_settings.preferences_pb"

@Module
object SettingsDataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = {
                context.preferencesDataStoreFile(PREFERENCES_DATA_STORE_FILE_NAME)
            }
        )
    }
}