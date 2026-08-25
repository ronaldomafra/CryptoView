package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val cryptoNetworkJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

internal fun HttpClientConfig<*>.configureCryptoHttpClient(
    config: CryptoProcessConfig,
    networkLogger: Logger,
) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(cryptoNetworkJson)
    }
    install(ContentEncoding) {
        gzip()
    }
    install(HttpTimeout) {
        connectTimeoutMillis = config.connectTimeoutMillis
        requestTimeoutMillis = config.requestTimeoutMillis
        socketTimeoutMillis = config.socketTimeoutMillis
    }
    install(Logging) {
        logger = networkLogger
        level = LogLevel.HEADERS
        sanitizeHeader { header -> header.isSensitiveCryptoHeader() }
    }
}

private fun String.isSensitiveCryptoHeader(): Boolean =
    equals(HttpHeaders.Authorization, ignoreCase = true) ||
        equals(HttpHeaders.Cookie, ignoreCase = true) ||
        equals(HttpHeaders.SetCookie, ignoreCase = true) ||
        equals(COIN_MARKET_CAP_API_KEY_HEADER, ignoreCase = true) ||
        contains("token", ignoreCase = true) ||
        contains("secret", ignoreCase = true) ||
        contains("password", ignoreCase = true)

private const val COIN_MARKET_CAP_API_KEY_HEADER = "X-CMC_PRO_API_KEY"
