package br.com.rmf.kmp.cryptoview

import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import br.com.rmf.kmp.cryptoview.utils.defaultCryptoProcessConfig
import br.com.rmf.kmp.cryptoview.data.api.configureCryptoHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.logging.Logger
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module
import platform.Foundation.NSLog
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun setPlatformHttpClient(config: CryptoProcessConfig): HttpClient = HttpClient(Darwin) {
    configureCryptoHttpClient(
        config = config,
        networkLogger = object : Logger {
            override fun log(message: String) {
                NSLog("CryptoViewHttp: %@", message)
            }
        },
    )
}

actual val platformModule = module {
    single {
        defaultCryptoProcessConfig(
            databaseWriteParallelism = 1,
            databasePoolSize = 1,
        )
    }
    single<HttpClient> { setPlatformHttpClient(get()) } withOptions {
        onClose { client -> client?.close() }
    }
}
