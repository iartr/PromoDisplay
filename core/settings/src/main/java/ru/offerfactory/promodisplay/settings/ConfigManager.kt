package ru.offerfactory.promodisplay.settings

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.settings.domain.model.ConfigModel

interface ConfigManager {
    suspend fun saveConfig(configModel: ConfigModel){
    }
    suspend fun getConfig(): Flow<ConfigModel>{
        TODO()
    }
}