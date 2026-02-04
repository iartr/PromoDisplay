package ru.offerfactory.promodisplay.settings

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.settings.model.ConfigModel


interface ConfigManager {

    suspend fun saveConfig(configModel: ConfigModel){
        TODO()
    }

    suspend fun getConfig(): Flow<ConfigModel>{
        TODO()
    }

}