package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.ApiResponseMetadata
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaErrorDto
import de.jensklingenberg.ktorfit.Response
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

internal class CoinPaprikaRequestExecutor {
    fun <T> execute(request: () -> Flow<Response<T>>): Flow<ApiResult<T>> = flow<ApiResult<T>> {
        request().collect { response -> emit(handleResponse(response)) }
    }.catch { exception ->
        when (exception) {
            is CancellationException -> throw exception
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException,
            -> emit(ApiResult.Failure(CryptoError.Timeout))

            is ContentConvertException,
            is SerializationException,
            -> emit(ApiResult.Failure(CryptoError.Serialization(exception.message)))

            is IOException -> emit(ApiResult.Failure(CryptoError.NoConnection))
            else -> emit(ApiResult.Failure(CryptoError.Unknown(exception.message)))
        }
    }

    private suspend fun <T> handleResponse(response: Response<T>): ApiResult<T> {
        if (!response.isSuccessful) {
            val message = readErrorMessage(response.errorBody())
            val error = when (response.code) {
                HTTP_NOT_FOUND -> CryptoError.NotFound(message)
                HTTP_TOO_MANY_REQUESTS -> CryptoError.RateLimited(
                    retryAfterMillis = response.headers[HttpHeaders.RetryAfter]
                        ?.toLongOrNull()?.times(MILLIS_PER_SECOND),
                    retryAfterRaw = response.headers[HttpHeaders.RetryAfter],
                    apiMessage = message,
                )
                in HTTP_SERVER_ERROR_RANGE -> CryptoError.ServerUnavailable(response.code)
                else -> CryptoError.InvalidResponse(message ?: "HTTP ${response.code}")
            }
            return ApiResult.Failure(error)
        }

        return response.body()?.let {
            ApiResult.Success(it, ApiResponseMetadata(null, null, null, null))
        } ?: ApiResult.Failure(CryptoError.InvalidResponse("Response body is empty"))
    }

    private suspend fun readErrorMessage(errorBody: Any?): String? {
        val bodyText = when (errorBody) {
            is String -> errorBody
            is ByteReadChannel -> errorBody.readRemaining().readString()
            is Source -> errorBody.readString()
            else -> return null
        }
        if (bodyText.isBlank()) return null
        return runCatching {
            cryptoNetworkJson.decodeFromString<CoinPaprikaErrorDto>(bodyText).error
        }.getOrNull()
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val MILLIS_PER_SECOND = 1_000L
        val HTTP_SERVER_ERROR_RANGE = 500..599
    }
}
