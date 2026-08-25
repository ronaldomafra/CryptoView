package br.com.rmf.kmp.cryptoview.domain.model

sealed interface ApiResult<out T> {
    data class Success<T>(
        val data: T,
        val metadata: ApiResponseMetadata,
    ) : ApiResult<T>

    data class Failure(val error: CryptoError) : ApiResult<Nothing>
}

data class ApiResponseMetadata(
    val timestamp: String?,
    val elapsed: Int?,
    val creditCount: Int?,
    val notice: String?,
)

sealed interface CryptoError {
    data object NoConnection : CryptoError
    data object Timeout : CryptoError
    data object MissingApiKey : CryptoError
    data class InvalidApiKey(val apiMessage: String? = null) : CryptoError
    data class PlanUnavailable(val apiMessage: String? = null) : CryptoError
    data class RateLimited(
        val retryAfterMillis: Long? = null,
        val retryAfterRaw: String? = null,
        val apiMessage: String? = null,
    ) : CryptoError

    data class ServerUnavailable(val statusCode: Int) : CryptoError
    data class InvalidResponse(val detail: String? = null) : CryptoError
    data class Serialization(val detail: String? = null) : CryptoError
    data class Unknown(val detail: String? = null) : CryptoError
}
