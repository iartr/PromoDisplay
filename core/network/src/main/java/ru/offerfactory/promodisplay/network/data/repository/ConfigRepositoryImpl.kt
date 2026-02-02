package ru.offerfactory.promodisplay.network.data.repository

import javax.inject.Inject
import ru.offerfactory.promodisplay.network.data.remote.ConfigApi
import ru.offerfactory.promodisplay.network.domain.model.ConfigInfo
import ru.offerfactory.promodisplay.network.domain.repository.ConfigRepository

class ConfigRepositoryImpl @Inject constructor(
    private val api: ConfigApi
) : ConfigRepository {

    override suspend fun getConfigFromApi(): Result<ConfigInfo> {
        TODO("Not yet implemented")
    }
}
