package br.com.rmf.kmp.cryptoview

import io.ktor.client.HttpClient
import org.koin.core.module.Module

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun setPlatformHttpClient(): HttpClient

expect val platformModule: Module

expect fun currentTimeMillis(): Long
