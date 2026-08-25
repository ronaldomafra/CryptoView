package br.com.rmf.kmp.cryptoview.di

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import br.com.rmf.kmp.cryptoview.security.PlatformApiKeyCipher
import br.com.rmf.kmp.cryptoview.security.SECURE_DATA_STORE_FILE_NAME
import br.com.rmf.kmp.cryptoview.security.createSecureDataStore
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.dsl.module
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory

@OptIn(ExperimentalForeignApi::class)
internal fun iosSecurityModule(cipher: PlatformApiKeyCipher) = module {
    single<PlatformApiKeyCipher> { cipher }
    single {
        val applicationSupportDirectory =
            "${NSHomeDirectory()}/Library/Application Support/br.com.rmf.kmp.cryptoview"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = applicationSupportDirectory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        createSecureDataStore(
            storage = OkioStorage<Preferences>(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = {
                    "$applicationSupportDirectory/$SECURE_DATA_STORE_FILE_NAME".toPath()
                },
            ),
        )
    }
}
