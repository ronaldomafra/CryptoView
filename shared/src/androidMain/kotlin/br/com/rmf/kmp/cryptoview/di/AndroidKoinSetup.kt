package br.com.rmf.kmp.cryptoview.di

import android.content.Context
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import br.com.rmf.kmp.cryptoview.security.AndroidPlatformApiKeyCipher
import br.com.rmf.kmp.cryptoview.security.PlatformApiKeyCipher
import br.com.rmf.kmp.cryptoview.security.SECURE_DATA_STORE_FILE_NAME
import br.com.rmf.kmp.cryptoview.security.createSecureDataStore
import org.koin.dsl.module
import okio.FileSystem
import okio.Path.Companion.toPath

fun initKoinAndroid(context: Context) {
    val applicationContext = context.applicationContext
    val androidSecurityModule = module {
        single<PlatformApiKeyCipher> { AndroidPlatformApiKeyCipher() }
        single {
            createSecureDataStore(
                storage = OkioStorage<Preferences>(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = {
                        applicationContext.filesDir
                            .resolve(SECURE_DATA_STORE_FILE_NAME)
                            .absolutePath
                            .toPath()
                    },
                ),
            )
        }
    }
    initKoin(listOf(androidSecurityModule))
}
