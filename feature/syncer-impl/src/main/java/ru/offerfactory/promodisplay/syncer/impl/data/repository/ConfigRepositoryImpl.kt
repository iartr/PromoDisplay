package ru.offerfactory.promodisplay.syncer.impl.data.repository

import ru.offerfactory.promodisplay.logger.AppLogger
import ru.offerfactory.promodisplay.network.domain.util.NetworkExceptionMapper.toNetworkError
import ru.offerfactory.promodisplay.network.domain.util.NetworkResult
import ru.offerfactory.promodisplay.syncer.impl.data.mappers.ConfigMappers
import ru.offerfactory.promodisplay.syncer.impl.data.remote.ConfigApi
import ru.offerfactory.promodisplay.syncer.impl.domain.repository.ConfigRepository
import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity
import javax.inject.Inject

class ConfigRepositoryImpl @Inject constructor(
    private val api: ConfigApi,
    private val mappers: ConfigMappers,
    private val appLogger: AppLogger
) : ConfigRepository {

    override suspend fun fetchConfig(): NetworkResult<ConfigEntity> {
        return try {
            val dto = api.getConfig()
            val config = mappers.run { dto.toDomain() }
            NetworkResult.Success(config)
        } catch (e: Exception) {
            val networkError = e.toNetworkError()
            appLogger.logError(networkError.throwable)
            NetworkResult.Failure(networkError)
        }
    }
}