package ru.offerfactory.promodisplay.syncer.impl.domain.repository

import ru.offerfactory.promodisplay.network.domain.util.NetworkResult
import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity

interface ConfigRepository {
    suspend fun fetchConfig(): NetworkResult<ConfigEntity>
}