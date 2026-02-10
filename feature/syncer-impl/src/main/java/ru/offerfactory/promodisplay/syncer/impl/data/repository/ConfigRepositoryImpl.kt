package ru.offerfactory.promodisplay.syncer.impl.data.repository

import ru.offerfactory.promodisplay.network.domain.error.NetworkError
import ru.offerfactory.promodisplay.network.domain.util.NetworkResult
import ru.offerfactory.promodisplay.syncer.impl.data.mappers.ConfigMappers
import javax.inject.Inject
import ru.offerfactory.promodisplay.syncer.impl.data.remote.ConfigApi
import ru.offerfactory.promodisplay.syncer.impl.domain.model.ConfigInfo
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository

class ConfigRepositoryImpl(
    private val api: ConfigApi,
    private val mappers: ConfigMappers
) : ConfigRepository {

    override suspend fun fetchConfig(): NetworkResult<ConfigInfo> {
        return try {
            val dto = api.getConfig()
            val config = mappers.run { dto.toDomain() }
            NetworkResult.Success(config)
        } catch (e: Exception) {
            val networkError = NetworkError.UnknownNetworkError(e)
            NetworkResult.Failure(networkError)
        }
    }
}