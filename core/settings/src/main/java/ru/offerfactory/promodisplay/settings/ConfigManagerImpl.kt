package ru.offerfactory.promodisplay.settings

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity

class ConfigManagerImpl: ConfigManager {
    override suspend fun getConfig(): Flow<ConfigEntity> {
        TODO()
    }
    override suspend fun saveConfig(configEntity: ConfigEntity) {
        TODO()
    }
}