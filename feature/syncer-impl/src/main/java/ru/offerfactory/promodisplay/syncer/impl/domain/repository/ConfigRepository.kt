package ru.offerfactory.promodisplay.syncer.impl.domain.repository

import ru.offerfactory.promodisplay.syncer.impl.domain.model.ConfigInfo

interface ConfigRepository {
    suspend fun getConfigFromApi(): Result<ConfigInfo>
}