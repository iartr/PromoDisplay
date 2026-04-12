package ru.offerfactory.promodisplay.network.domain.error

sealed class NetworkError(open val throwable: Throwable) {

    data class NoInternetError(override val throwable: Throwable) : NetworkError(throwable)

    data class TimeoutError(override val throwable: Throwable) : NetworkError(throwable)

    data class UnknownNetworkError(override val throwable: Throwable) : NetworkError(throwable)

    data class ClientError(
        val code: Int,
        val message: String?,
        override val throwable: Throwable
    ) : NetworkError(throwable)

    data class ServerError(
        val code: Int,
        val message: String?,
        override val throwable: Throwable
    ) : NetworkError(throwable)
}