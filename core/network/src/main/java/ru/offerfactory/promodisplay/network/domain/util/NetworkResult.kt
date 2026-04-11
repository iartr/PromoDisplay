package ru.offerfactory.promodisplay.network.domain.util

import ru.offerfactory.promodisplay.network.domain.error.NetworkError

sealed class NetworkResult<out T> {

    data class Success<out T>(val data: T) : NetworkResult<T>()

    data class Failure(val error: NetworkError) : NetworkResult<Nothing>()
}