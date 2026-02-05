package ru.offerfactory.promodisplay.settings

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.settings.domain.model.ConfigModel

class ConfigManagerImpl: ConfigManager {
    override suspend fun getConfig(): Flow<ConfigModel> {
        TODO()
    }
    override suspend fun saveConfig(configModel: ConfigModel) {
        TODO()
    }
}