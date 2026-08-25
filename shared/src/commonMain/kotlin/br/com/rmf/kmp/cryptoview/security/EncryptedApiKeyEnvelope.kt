package br.com.rmf.kmp.cryptoview.security

data class EncryptedApiKeyEnvelope(
    val version: Int,
    val nonceBase64: String,
    val cipherTextAndTagBase64: String,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
