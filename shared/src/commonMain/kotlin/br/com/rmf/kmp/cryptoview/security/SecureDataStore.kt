package br.com.rmf.kmp.cryptoview.security

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences

internal const val SECURE_DATA_STORE_FILE_NAME = "cryptoview_secure.preferences_pb"

internal fun createSecureDataStore(
    storage: Storage<Preferences>,
): DataStore<Preferences> = DataStoreFactory.create(storage = storage)
