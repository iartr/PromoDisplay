package ru.offerfactory.promodisplay.settings

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity
import javax.inject.Inject
class ConfigManagerImpl @Inject constructor(): ConfigManager {
    override suspend fun getConfig(): Flow<ConfigEntity> {
        TODO()
    }
    override suspend fun saveConfig(configEntity: ConfigEntity) {
        TODO()
    }
}