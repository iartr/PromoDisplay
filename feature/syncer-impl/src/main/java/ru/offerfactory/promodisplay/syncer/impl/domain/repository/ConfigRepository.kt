package ru.offerfactory.promodisplay.syncer.impl.domain.repository

import ru.offerfactory.promodisplay.network.domain.util.NetworkResult
import ru.offerfactory.promodisplay.syncer.impl.domain.model.ConfigInfo

interface ConfigRepository {
    suspend fun fetchConfig(): NetworkResult<ConfigInfo>
}