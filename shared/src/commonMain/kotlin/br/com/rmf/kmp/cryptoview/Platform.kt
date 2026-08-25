package br.com.rmf.kmp.cryptoview

import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import io.ktor.client.HttpClient
import org.koin.core.module.Module

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun setPlatformHttpClient(config: CryptoProcessConfig): HttpClient

expect val platformModule: Module

expect fun currentTimeMillis(): Long
