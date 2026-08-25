package br.com.rmf.kmp.cryptoview.security

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultSecureApiKeyStorageTest {
    @Test
    fun saveAndReadRoundTripUsesOnlyEncryptedEnvelopeInStore() = runTest {
        val cipher = FakeCipher()
        val store = FakeEnvelopeStore()
        val storage = DefaultSecureApiKeyStorage(cipher, store)

        assertIs<SecureApiKeyResult.Success<Unit>>(storage.save(API_KEY))
        assertEquals(ENVELOPE, store.envelope)
        assertEquals(API_KEY, assertIs<SecureApiKeyResult.Success<String>>(storage.read()).value)
        assertEquals(ENVELOPE, cipher.lastDecryptedEnvelope)
    }

    @Test
    fun blankValueIsRejectedBeforeEncryption() = runTest {
        val cipher = FakeCipher()
        val storage = DefaultSecureApiKeyStorage(cipher, FakeEnvelopeStore())

        val result = assertIs<SecureApiKeyResult.Failure>(storage.save("   "))

        assertEquals(SecureApiKeyError.INVALID_INPUT, result.error)
        assertNull(cipher.lastPlainText)
    }

    @Test
    fun encryptionFailureDoesNotReplaceStoredEnvelope() = runTest {
        val previousEnvelope = ENVELOPE.copy(nonceBase64 = "previous")
        val store = FakeEnvelopeStore(previousEnvelope)
        val cipher = FakeCipher(encryptionResult = null)
        val storage = DefaultSecureApiKeyStorage(cipher, store)

        val result = assertIs<SecureApiKeyResult.Failure>(storage.save(API_KEY))

        assertEquals(SecureApiKeyError.ENCRYPTION_FAILED, result.error)
        assertEquals(previousEnvelope, store.envelope)
    }

    @Test
    fun decryptionFailureClearsEnvelopeAndNativeKey() = runTest {
        val store = FakeEnvelopeStore(ENVELOPE)
        val cipher = FakeCipher(decryptionResult = null)
        val storage = DefaultSecureApiKeyStorage(cipher, store)

        val result = assertIs<SecureApiKeyResult.Failure>(storage.read())

        assertEquals(SecureApiKeyError.DECRYPTION_FAILED, result.error)
        assertNull(store.envelope)
        assertTrue(cipher.keyDeleted)
    }

    @Test
    fun statusDistinguishesMissingConfiguredAndCorruptedEnvelope() = runTest {
        val store = FakeEnvelopeStore()
        val storage = DefaultSecureApiKeyStorage(FakeCipher(), store)
        assertEquals(SecureApiKeyStatus.NOT_CONFIGURED, storage.status())

        store.envelope = ENVELOPE
        assertEquals(SecureApiKeyStatus.CONFIGURED, storage.status())

        store.corrupted = true
        assertEquals(SecureApiKeyStatus.RECOVERY_REQUIRED, storage.status())
    }

    @Test
    fun removeClearsEnvelopeThenDeletesNativeKey() = runTest {
        val events = mutableListOf<String>()
        val store = FakeEnvelopeStore(ENVELOPE, events = events)
        val cipher = FakeCipher(events = events)
        val storage = DefaultSecureApiKeyStorage(cipher, store)

        assertIs<SecureApiKeyResult.Success<Unit>>(storage.remove())

        assertEquals(listOf("clear-envelope", "delete-key"), events)
        assertNull(store.envelope)
    }

    private class FakeCipher(
        private val encryptionResult: EncryptedApiKeyEnvelope? = ENVELOPE,
        private val decryptionResult: String? = API_KEY,
        private val events: MutableList<String>? = null,
    ) : PlatformApiKeyCipher {
        var lastPlainText: String? = null
        var lastDecryptedEnvelope: EncryptedApiKeyEnvelope? = null
        var keyDeleted = false

        override fun encrypt(plainText: String): EncryptedApiKeyEnvelope? {
            lastPlainText = plainText
            return encryptionResult
        }

        override fun decrypt(envelope: EncryptedApiKeyEnvelope): String? {
            lastDecryptedEnvelope = envelope
            return decryptionResult
        }

        override fun deleteKey(): Boolean {
            events?.add("delete-key")
            keyDeleted = true
            return true
        }
    }

    private class FakeEnvelopeStore(
        var envelope: EncryptedApiKeyEnvelope? = null,
        private val events: MutableList<String>? = null,
    ) : EncryptedApiKeyEnvelopeStore {
        var corrupted = false

        override suspend fun read(): StoredEnvelope = when {
            corrupted -> StoredEnvelope.Corrupted
            envelope == null -> StoredEnvelope.Missing
            else -> StoredEnvelope.Value(envelope!!)
        }

        override suspend fun write(envelope: EncryptedApiKeyEnvelope) {
            this.envelope = envelope
        }

        override suspend fun clear() {
            events?.add("clear-envelope")
            envelope = null
            corrupted = false
        }
    }

    private companion object {
        const val API_KEY = "test-api-key-never-log-this"
        val ENVELOPE = EncryptedApiKeyEnvelope(
            version = EncryptedApiKeyEnvelope.CURRENT_VERSION,
            nonceBase64 = "bm9uY2U=",
            cipherTextAndTagBase64 = "Y2lwaGVydGV4dC10YWc=",
        )
    }
}
