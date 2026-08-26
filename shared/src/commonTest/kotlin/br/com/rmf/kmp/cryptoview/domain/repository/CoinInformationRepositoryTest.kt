package br.com.rmf.kmp.cryptoview.domain.repository

import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaCurrencyDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoinInformationRepositoryTest {
    @Test
    fun `selects bitcoin and ignores partial or unrelated BTC symbols`() {
        val result = selectCoinPaprikaCandidate(
            coin = coin(name = "Bitcoin", symbol = "BTC", slug = "bitcoin"),
            candidates = listOf(
                candidate("btc-bitcoin", "Bitcoin", "BTC", rank = 1),
                candidate("cbbtc-coinbase-wrapped-btc", "Coinbase Wrapped BTC", "CBBTC", rank = 22),
                candidate("lbtc-lombard-staked-btc", "Lombard Staked BTC", "LBTC", rank = 79),
                candidate("btcb-bitcoin-avalanche-bridged-btcb", "Bitcoin Avalanche Bridged", "BTC.B", rank = 129),
                candidate("btc-bobby-the-cat", "Bobby The Cat", "BTC", rank = 4_653),
            ),
        )

        assertEquals("btc-bitcoin", result?.id)
    }

    @Test
    fun `uses the provider id slug when display names differ`() {
        val result = selectCoinPaprikaCandidate(
            coin = coin(name = "Polygon (MATIC)", symbol = "MATIC", slug = "polygon"),
            candidates = listOf(
                candidate("matic-polygon", "Polygon Ecosystem Token", "MATIC", rank = 50),
            ),
        )

        assertEquals("matic-polygon", result?.id)
    }

    @Test
    fun `does not guess by rank when identity does not match`() {
        val result = selectCoinPaprikaCandidate(
            coin = coin(name = "Original Coin", symbol = "ONE", slug = "original-coin"),
            candidates = listOf(
                candidate("one-harmony", "Harmony", "ONE", rank = 10),
            ),
        )

        assertNull(result)
    }

    @Test
    fun `rejects inactive exact match`() {
        val result = selectCoinPaprikaCandidate(
            coin = coin(name = "Bitcoin", symbol = "BTC", slug = "bitcoin"),
            candidates = listOf(
                candidate("btc-bitcoin", "Bitcoin", "BTC", rank = 1, active = false),
            ),
        )

        assertNull(result)
    }

    @Test
    fun `rejects more than one strict match`() {
        val result = selectCoinPaprikaCandidate(
            coin = coin(name = "Bitcoin", symbol = "BTC", slug = "bitcoin"),
            candidates = listOf(
                candidate("btc-bitcoin", "Bitcoin", "BTC", rank = 1),
                candidate("btc-bitcoin-copy", "Bitcoin", "BTC", rank = 2),
            ),
        )

        assertNull(result)
    }

    private fun coin(name: String, symbol: String, slug: String) = CoinSummary(
        id = 1,
        name = name,
        symbol = symbol,
        slug = slug,
        rank = 1,
        numMarketPairs = null,
        priceUsd = null,
        volume24hUsd = null,
        percentChange24h = null,
        marketCapUsd = null,
        quoteUpdatedAt = null,
        quoteFetchedAt = null,
        logoUrl = null,
    )

    private fun candidate(
        id: String,
        name: String,
        symbol: String,
        rank: Long,
        active: Boolean = true,
    ) = CoinPaprikaCurrencyDto(
        id = id,
        name = name,
        symbol = symbol,
        rank = rank,
        isActive = active,
        type = "coin",
    )
}
