package ru.offerfactory.promodisplay.settings

import kotlinx.serialization.json.Json
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity
import javax.inject.Inject

class ConfigManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ConfigManager {

    companion object {
        private val KEY_CONFIG = stringPreferencesKey("config_json")
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false

        }
    }

    override suspend fun saveConfig(configEntity: ConfigEntity) {
        dataStore.edit { preferences ->
            preferences[KEY_CONFIG] = json.encodeToString(configEntity)
        }
    }

    override fun getConfig(): Flow<ConfigEntity> {
        return dataStore.data.map { preferences ->
            val jsonString = preferences[KEY_CONFIG]
                ?: error("Config not found")

            runCatching {
                json.decodeFromString<ConfigEntity>(jsonString)
            }.getOrElse {
                error("Invalid config format")
            }
        }
    }
}