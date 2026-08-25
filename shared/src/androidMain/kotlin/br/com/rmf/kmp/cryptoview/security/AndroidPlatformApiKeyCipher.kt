package br.com.rmf.kmp.cryptoview.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AndroidPlatformApiKeyCipher : PlatformApiKeyCipher {
    override fun encrypt(plainText: String): EncryptedApiKeyEnvelope? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val cipherTextAndTag = cipher.doFinal(plainText.encodeToByteArray())

        EncryptedApiKeyEnvelope(
            version = EncryptedApiKeyEnvelope.CURRENT_VERSION,
            nonceBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            cipherTextAndTagBase64 = Base64.encodeToString(cipherTextAndTag, Base64.NO_WRAP),
        )
    }.getOrNull()

    override fun decrypt(envelope: EncryptedApiKeyEnvelope): String? = runCatching {
        require(envelope.version == EncryptedApiKeyEnvelope.CURRENT_VERSION)
        val nonce = Base64.decode(envelope.nonceBase64, Base64.NO_WRAP)
        val cipherTextAndTag = Base64.decode(envelope.cipherTextAndTagBase64, Base64.NO_WRAP)
        require(nonce.size == GCM_NONCE_SIZE_BYTES)
        require(cipherTextAndTag.size >= GCM_TAG_SIZE_BYTES)

        val keyStore = loadKeyStore()
        val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_SIZE_BITS, nonce),
        )
        cipher.doFinal(cipherTextAndTag).decodeToString()
    }.getOrNull()

    override fun deleteKey(): Boolean = runCatching {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
        true
    }.getOrDefault(false)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = loadKeyStore()
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(AES_KEY_SIZE_BITS)
                        .setRandomizedEncryptionRequired(true)
                        .build(),
                )
            }
            .generateKey()
    }

    private fun loadKeyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "br.com.rmf.kmp.cryptoview.api-key.aes.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_SIZE_BITS = 256
        const val GCM_TAG_SIZE_BITS = 128
        const val GCM_TAG_SIZE_BYTES = GCM_TAG_SIZE_BITS / 8
        const val GCM_NONCE_SIZE_BYTES = 12
    }
}
