package ru.offerfactory.promodisplay.settings

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity
interface ConfigManager {
    suspend fun saveConfig(configEntity: ConfigEntity)
    fun getConfig(): Flow<ConfigEntity>
}