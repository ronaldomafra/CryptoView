package br.com.rmf.kmp.cryptoview

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform