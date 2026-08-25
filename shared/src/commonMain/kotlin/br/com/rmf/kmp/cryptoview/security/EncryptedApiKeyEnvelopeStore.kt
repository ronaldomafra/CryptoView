package br.com.rmf.kmp.cryptoview.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

internal interface EncryptedApiKeyEnvelopeStore {
    suspend fun read(): StoredEnvelope

    suspend fun write(envelope: EncryptedApiKeyEnvelope)

    suspend fun clear()
}

internal sealed interface StoredEnvelope {
    data object Missing : StoredEnvelope
    data object Corrupted : StoredEnvelope
    data class Value(val envelope: EncryptedApiKeyEnvelope) : StoredEnvelope
}

internal class DataStoreEncryptedApiKeyEnvelopeStore(
    private val dataStore: DataStore<Preferences>,
) : EncryptedApiKeyEnvelopeStore {
    override suspend fun read(): StoredEnvelope {
        val preferences = dataStore.data.first()
        val version = preferences[VERSION]
        val nonce = preferences[NONCE]
        val cipherTextAndTag = preferences[CIPHER_TEXT_AND_TAG]

        if (version == null && nonce == null && cipherTextAndTag == null) {
            return StoredEnvelope.Missing
        }
        if (version == null || nonce.isNullOrBlank() || cipherTextAndTag.isNullOrBlank()) {
            return StoredEnvelope.Corrupted
        }

        return StoredEnvelope.Value(
            EncryptedApiKeyEnvelope(
                version = version,
                nonceBase64 = nonce,
                cipherTextAndTagBase64 = cipherTextAndTag,
            ),
        )
    }

    override suspend fun write(envelope: EncryptedApiKeyEnvelope) {
        dataStore.edit { preferences ->
            preferences[VERSION] = envelope.version
            preferences[NONCE] = envelope.nonceBase64
            preferences[CIPHER_TEXT_AND_TAG] = envelope.cipherTextAndTagBase64
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(VERSION)
            preferences.remove(NONCE)
            preferences.remove(CIPHER_TEXT_AND_TAG)
        }
    }

    private companion object {
        val VERSION = intPreferencesKey("api_key_envelope_version")
        val NONCE = stringPreferencesKey("api_key_envelope_nonce")
        val CIPHER_TEXT_AND_TAG = stringPreferencesKey("api_key_envelope_cipher_text_and_tag")
    }
}
