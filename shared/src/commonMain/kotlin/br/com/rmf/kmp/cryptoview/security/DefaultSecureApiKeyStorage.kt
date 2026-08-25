package br.com.rmf.kmp.cryptoview.security

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

internal class DefaultSecureApiKeyStorage(
    private val cipher: PlatformApiKeyCipher,
    private val envelopeStore: EncryptedApiKeyEnvelopeStore,
) : SecureApiKeyStorage {
    private val mutex = Mutex()

    override suspend fun save(apiKey: String): SecureApiKeyResult<Unit> = mutex.withLock {
        if (apiKey.isBlank()) {
            return@withLock SecureApiKeyResult.Failure(SecureApiKeyError.INVALID_INPUT)
        }

        val envelope = cipher.encrypt(apiKey)
            ?: return@withLock SecureApiKeyResult.Failure(SecureApiKeyError.ENCRYPTION_FAILED)

        try {
            envelopeStore.write(envelope)
            SecureApiKeyResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            SecureApiKeyResult.Failure(SecureApiKeyError.PERSISTENCE_FAILED)
        }
    }

    override suspend fun read(): SecureApiKeyResult<String> = mutex.withLock {
        val stored = try {
            envelopeStore.read()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            return@withLock SecureApiKeyResult.Failure(SecureApiKeyError.PERSISTENCE_FAILED)
        }

        when (stored) {
            StoredEnvelope.Missing -> SecureApiKeyResult.Failure(SecureApiKeyError.NOT_CONFIGURED)
            StoredEnvelope.Corrupted -> recoverFromInvalidState()
            is StoredEnvelope.Value -> {
                if (stored.envelope.version != EncryptedApiKeyEnvelope.CURRENT_VERSION) {
                    recoverFromInvalidState()
                } else {
                    val plainText = cipher.decrypt(stored.envelope)
                    if (plainText.isNullOrBlank()) {
                        recoverFromInvalidState()
                    } else {
                        SecureApiKeyResult.Success(plainText)
                    }
                }
            }
        }
    }

    override suspend fun status(): SecureApiKeyStatus = mutex.withLock {
        try {
            when (val stored = envelopeStore.read()) {
                StoredEnvelope.Missing -> SecureApiKeyStatus.NOT_CONFIGURED
                StoredEnvelope.Corrupted -> SecureApiKeyStatus.RECOVERY_REQUIRED
                is StoredEnvelope.Value -> if (
                    stored.envelope.version == EncryptedApiKeyEnvelope.CURRENT_VERSION
                ) {
                    SecureApiKeyStatus.CONFIGURED
                } else {
                    SecureApiKeyStatus.RECOVERY_REQUIRED
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            SecureApiKeyStatus.UNAVAILABLE
        }
    }

    override suspend fun remove(): SecureApiKeyResult<Unit> = mutex.withLock {
        try {
            envelopeStore.clear()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            return@withLock SecureApiKeyResult.Failure(SecureApiKeyError.PERSISTENCE_FAILED)
        }

        if (cipher.deleteKey()) {
            SecureApiKeyResult.Success(Unit)
        } else {
            SecureApiKeyResult.Failure(SecureApiKeyError.KEY_DELETION_FAILED)
        }
    }

    private suspend fun recoverFromInvalidState(): SecureApiKeyResult.Failure {
        try {
            envelopeStore.clear()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            // Recovery still attempts to invalidate the native key.
        }
        cipher.deleteKey()
        return SecureApiKeyResult.Failure(SecureApiKeyError.DECRYPTION_FAILED)
    }
}
