package br.com.rmf.kmp.cryptoview.domain.repository

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRemoteDataSource
import br.com.rmf.kmp.cryptoview.data.database.MarketLocalDataSource
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinExchangeMarket
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryPoint
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryRange
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.CoinSortOrder
import br.com.rmf.kmp.cryptoview.domain.model.CoinVariationFilter
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeAsset
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope

data class CoinDetailRefreshResult(
    val quoteError: CryptoError? = null,
    val marketsError: CryptoError? = null,
)

class MarketRepository internal constructor(
    private val local: MarketLocalDataSource,
    private val remote: CoinMarketCapRemoteDataSource,
) {
    fun observeCoins(
        query: String,
        limit: Int,
        sortOrder: CoinSortOrder = CoinSortOrder.MARKET_CAP,
        variation: CoinVariationFilter = CoinVariationFilter.ALL,
        exchangeId: Long? = null,
        offset: Int = 0,
    ): Flow<List<CoinSummary>> = local.observeCoins(
        query = query,
        limit = limit.toLong(),
        sortOrder = sortOrder,
        variation = variation,
        exchangeId = exchangeId,
        offset = offset.toLong(),
    )

    fun observeExchanges(query: String, limit: Int, offset: Int = 0): Flow<List<ExchangeSummary>> =
        local.observeExchanges(query, limit.toLong(), offset.toLong())

    fun observeCachedMarketExchanges(limit: Int): Flow<List<ExchangeSummary>> =
        local.observeCachedMarketExchanges(limit.toLong())

    fun observeCoin(id: Long): Flow<CoinSummary?> = local.observeCoin(id)
    fun observeExchange(id: Long): Flow<ExchangeSummary?> = local.observeExchange(id)
    fun observeAssets(exchangeId: Long): Flow<List<ExchangeAsset>> = local.observeAssets(exchangeId)
    fun observeMarkets(coinId: Long): Flow<List<CoinExchangeMarket>> = local.observeMarkets(coinId)
    fun observeHistory(coinId: Long, range: CoinHistoryRange): Flow<List<CoinHistoryPoint>> =
        local.observeHistory(coinId, range)
    fun coinDescription(id: Long): Pair<String?, String?> = local.coinDescription(id)

    fun coinCount(): Long = local.countCoins()
    fun exchangeCount(): Long = local.countExchanges()

    suspend fun refreshExchangeAssets(exchangeId: Long, force: Boolean = false): CryptoError? {
        val key = "exchange_assets:$exchangeId"
        if (!force && local.isFresh(key, METADATA_CACHE_TTL_MILLIS)) return null
        return when (val result = remote.exchangeAssets(exchangeId).first()) {
            is ApiResult.Success -> {
                local.replaceAssets(exchangeId, result.data)
                null
            }
            is ApiResult.Failure -> {
                local.markResourceFailure(key, result.error::class.simpleName ?: "error")
                result.error
            }
        }
    }

    suspend fun refreshCoinDetails(coinId: Long): CoinDetailRefreshResult = supervisorScope {
        val quote = async { refreshQuote(coinId) }
        val markets = async { refreshMarkets(coinId) }
        CoinDetailRefreshResult(
            quoteError = quote.await(),
            marketsError = markets.await(),
        )
    }

    suspend fun refreshQuote(coinId: Long): CryptoError? =
        when (val result = remote.coinQuotes(listOf(coinId)).first()) {
            is ApiResult.Success -> {
                val quote = result.data[coinId.toString()] ?: result.data.values.firstOrNull()
                if (quote == null) {
                    CryptoError.InvalidResponse("Cotação ausente")
                } else {
                    local.replaceQuote(coinId, quote)
                    null
                }
            }
            is ApiResult.Failure -> result.error
        }

    suspend fun refreshMarkets(coinId: Long, force: Boolean = false): CryptoError? {
        val key = "coin_markets:$coinId"
        if (!force && local.isFresh(key, MARKET_PAIRS_CACHE_TTL_MILLIS)) return null
        return when (val result = remote.coinMarketPairs(coinId).first()) {
            is ApiResult.Success -> {
                local.replaceMarkets(coinId, result.data)
                null
            }
            is ApiResult.Failure -> {
                local.markResourceFailure(key, result.error::class.simpleName ?: "error")
                result.error
            }
        }
    }

    suspend fun refreshHistory(
        coinId: Long,
        range: CoinHistoryRange,
        force: Boolean = false,
    ): CryptoError? {
        val key = local.historyResourceKey(coinId, range)
        if (!force && local.isFresh(key, HISTORY_CACHE_TTL_MILLIS)) return null
        return when (val result = remote.coinHistory(coinId, range).first()) {
            is ApiResult.Success -> {
                local.replaceHistory(coinId, range, result.data)
                null
            }
            is ApiResult.Failure -> {
                local.markResourceFailure(key, result.error::class.simpleName ?: "error")
                result.error
            }
        }
    }

    suspend fun clearCache() = local.clear()

    private fun MarketLocalDataSource.isFresh(key: String, ttl: Long): Boolean {
        val lastSuccess = resourceLastSuccess(key) ?: return false
        return currentTimeMillis() - lastSuccess < ttl
    }

    private companion object {
        const val METADATA_CACHE_TTL_MILLIS = 24 * 60 * 60_000L
        const val MARKET_PAIRS_CACHE_TTL_MILLIS = 5 * 60_000L
        const val HISTORY_CACHE_TTL_MILLIS = 15 * 60_000L
    }
}
