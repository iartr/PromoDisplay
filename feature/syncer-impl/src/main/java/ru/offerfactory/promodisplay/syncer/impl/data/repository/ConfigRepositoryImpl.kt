package ru.offerfactory.promodisplay.syncer.impl.data.repository

import javax.inject.Inject
import ru.offerfactory.promodisplay.syncer.impl.data.remote.ConfigApi
import ru.offerfactory.promodisplay.syncer.impl.domain.model.ConfigInfo
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository

class ConfigRepositoryImpl @Inject constructor(
    private val api: ConfigApi
) : ConfigRepository {

    override suspend fun getConfigFromApi(): Result<ConfigInfo> {
        TODO("Not yet implemented")
    }
}