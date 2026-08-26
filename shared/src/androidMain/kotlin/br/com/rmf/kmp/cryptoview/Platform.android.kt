package br.com.rmf.kmp.cryptoview

import android.os.Build
import android.util.Log
import br.com.rmf.kmp.cryptoview.utils.defaultCryptoProcessConfig
import br.com.rmf.kmp.cryptoview.data.api.configureCryptoHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.Logger
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun setPlatformHttpClient(): HttpClient = HttpClient(CIO) {
    configureCryptoHttpClient(
        networkLogger = object : Logger {
            override fun log(message: String) {
                Log.d("CryptoViewHttp", message)
            }
        },
    )
}

actual val platformModule = module {
    single {
        defaultCryptoProcessConfig(
            parallelIoValue = 20,
            parallelDbValue = 2,
        )
    }
    single<HttpClient> { setPlatformHttpClient() } withOptions {
        onClose { client -> client?.close() }
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
