package ru.offerfactory.promodisplay.network.domain.repository

import ru.offerfactory.promodisplay.network.domain.model.ConfigInfo

interface ConfigRepository {
    suspend fun getConfigFromApi(): Result<ConfigInfo>
}