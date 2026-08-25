package br.com.rmf.kmp.cryptoview.security

/**
 * Boundary implemented with native security APIs on each platform.
 * Implementations must not throw across the Swift/Kotlin boundary.
 */
interface PlatformApiKeyCipher {
    fun encrypt(plainText: String): EncryptedApiKeyEnvelope?

    fun decrypt(envelope: EncryptedApiKeyEnvelope): String?

    fun deleteKey(): Boolean
}
