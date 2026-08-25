package br.com.rmf.kmp.cryptoview.security

interface SecureApiKeyStorage {
    suspend fun save(apiKey: String): SecureApiKeyResult<Unit>

    suspend fun read(): SecureApiKeyResult<String>

    suspend fun status(): SecureApiKeyStatus

    suspend fun remove(): SecureApiKeyResult<Unit>
}

enum class SecureApiKeyStatus {
    NOT_CONFIGURED,
    CONFIGURED,
    RECOVERY_REQUIRED,
    UNAVAILABLE,
}

sealed interface SecureApiKeyResult<out T> {
    data class Success<T>(val value: T) : SecureApiKeyResult<T>

    data class Failure(val error: SecureApiKeyError) : SecureApiKeyResult<Nothing>
}

enum class SecureApiKeyError {
    INVALID_INPUT,
    NOT_CONFIGURED,
    ENCRYPTION_FAILED,
    DECRYPTION_FAILED,
    PERSISTENCE_FAILED,
    KEY_DELETION_FAILED,
}
