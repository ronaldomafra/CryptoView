package br.com.rmf.kmp.cryptoview.data.database

import kotlin.test.Test
import kotlin.test.assertEquals

class CoinLogoUrlTest {
    @Test
    fun buildsOfficialLogoUrlFromCoinId() {
        assertEquals(
            "https://s2.coinmarketcap.com/static/img/coins/64x64/1.png",
            coinMarketCapLogoUrl(1),
        )
    }

    @Test
    fun cachedLogoTakesPrecedenceAndBlankCacheFallsBack() {
        assertEquals("https://cache.example/bitcoin.png", resolveCoinLogoUrl(1, "https://cache.example/bitcoin.png"))
        assertEquals(coinMarketCapLogoUrl(1), resolveCoinLogoUrl(1, " "))
        assertEquals(coinMarketCapLogoUrl(1), resolveCoinLogoUrl(1, null))
    }
}
