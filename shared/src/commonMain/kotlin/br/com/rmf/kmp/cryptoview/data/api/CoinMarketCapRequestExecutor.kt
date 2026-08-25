package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.ApiResponseMetadata
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.api.ApiEnvelope
import br.com.rmf.kmp.cryptoview.domain.model.api.ApiStatusDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ErrorEnvelope
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
import kotlinx.io.readString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

internal class CoinMarketCapRequestExecutor {
    fun <T> execute(
        request: () -> Flow<Response<ApiEnvelope<T>>>,
    ): Flow<ApiResult<T>> = flow<ApiResult<T>> {
        request().collect { response ->
            emit(handleResponse(response))
        }
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

    private suspend fun <T> handleResponse(
        response: Response<ApiEnvelope<T>>,
    ): ApiResult<T> {
        val retryAfterRaw = response.headers[HttpHeaders.RetryAfter]
        if (!response.isSuccessful) {
            return ApiResult.Failure(
                mapError(
                    httpStatusCode = response.code,
                    status = readErrorStatus(response.errorBody()),
                    retryAfterRaw = retryAfterRaw,
                ),
            )
        }

        val envelope = response.body()
            ?: return ApiResult.Failure(CryptoError.InvalidResponse("Response body is empty"))
        val errorCode = envelope.status.errorCode
            ?: return ApiResult.Failure(CryptoError.InvalidResponse("status.error_code is missing"))

        if (errorCode != API_SUCCESS_CODE) {
            return ApiResult.Failure(
                mapError(
                    httpStatusCode = response.code,
                    status = envelope.status,
                    retryAfterRaw = retryAfterRaw,
                ),
            )
        }

        val data = envelope.data
            ?: return ApiResult.Failure(CryptoError.InvalidResponse("Response data is missing"))

        return ApiResult.Success(
            data = data,
            metadata = ApiResponseMetadata(
                timestamp = envelope.status.timestamp,
                elapsed = envelope.status.elapsed,
                creditCount = envelope.status.creditCount,
                notice = envelope.status.notice,
            ),
        )
    }

    private fun mapError(
        httpStatusCode: Int,
        status: ApiStatusDto?,
        retryAfterRaw: String?,
    ): CryptoError {
        val apiMessage = status?.errorMessage
        val apiErrorCode = status?.errorCode

        if (httpStatusCode == HTTP_TOO_MANY_REQUESTS) {
            return rateLimited(retryAfterRaw, apiMessage)
        }
        if (httpStatusCode in HTTP_SERVER_ERROR_RANGE) {
            return CryptoError.ServerUnavailable(httpStatusCode)
        }

        return when (apiErrorCode) {
            1001, 1007 -> CryptoError.InvalidApiKey(apiMessage)
            1002, 1005 -> CryptoError.MissingApiKey
            1003, 1004, 1006 -> CryptoError.PlanUnavailable(apiMessage)
            in 1008..1011 -> rateLimited(retryAfterRaw, apiMessage)
            else -> when (httpStatusCode) {
                HTTP_UNAUTHORIZED -> CryptoError.InvalidApiKey(apiMessage)
                HTTP_FORBIDDEN -> CryptoError.PlanUnavailable(apiMessage)
                else -> CryptoError.InvalidResponse(apiMessage ?: "HTTP $httpStatusCode")
            }
        }
    }

    private fun rateLimited(retryAfterRaw: String?, apiMessage: String?) =
        CryptoError.RateLimited(
            retryAfterMillis = retryAfterRaw?.toLongOrNull()?.times(MILLIS_PER_SECOND),
            retryAfterRaw = retryAfterRaw,
            apiMessage = apiMessage,
        )

    private suspend fun readErrorStatus(errorBody: Any?): ApiStatusDto? {
        val bodyText = when (errorBody) {
            is String -> errorBody
            is ByteReadChannel -> errorBody.readRemaining().readString()
            else -> return null
        }
        if (bodyText.isBlank()) return null

        return runCatching {
            cryptoNetworkJson.decodeFromString<ErrorEnvelope>(bodyText).status
        }.getOrNull()
    }

    private companion object {
        const val API_SUCCESS_CODE = 0
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val MILLIS_PER_SECOND = 1_000L
        val HTTP_SERVER_ERROR_RANGE = 500..599
    }
}
