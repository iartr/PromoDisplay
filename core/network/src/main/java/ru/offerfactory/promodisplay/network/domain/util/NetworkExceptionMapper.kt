package ru.offerfactory.promodisplay.network.domain.util

import retrofit2.HttpException
import ru.offerfactory.promodisplay.network.domain.error.NetworkError
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkExceptionMapper {
    fun Throwable.toNetworkError(): NetworkError =
        when (this) {
            is UnknownHostException -> NetworkError.NoInternetError(this)

            is SocketTimeoutException -> NetworkError.TimeoutError(this)

            is HttpException -> {
                val code = this.code()

                if (code in 400..499) {
                    NetworkError.ClientError(code, this.message(), this)
                } else {
                    NetworkError.ServerError(code, this.message(), this)
                }
            }

            else -> NetworkError.UnknownNetworkError(this)
        }
}