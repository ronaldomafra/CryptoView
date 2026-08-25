package br.com.rmf.kmp.cryptoview.domain.repository

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRemoteDataSource
import br.com.rmf.kmp.cryptoview.data.database.MarketLocalDataSource
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinExchangeMarket
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryPoint
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeAsset
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeSummary
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope

data class CoinDetailRefreshResult(
    val quoteError: CryptoError? = null,
    val marketsError: CryptoError? = null,
    val historyError: CryptoError? = null,
)

class MarketRepository internal constructor(
    private val local: MarketLocalDataSource,
    private val remote: CoinMarketCapRemoteDataSource,
    private val config: CryptoProcessConfig,
) {
    fun observeCoins(query: String, limit: Int): Flow<List<CoinSummary>> =
        local.observeCoins(query, limit.toLong())

    fun observeExchanges(query: String, limit: Int): Flow<List<ExchangeSummary>> =
        local.observeExchanges(query, limit.toLong())

    fun observeCoin(id: Long): Flow<CoinSummary?> = local.observeCoin(id)
    fun observeExchange(id: Long): Flow<ExchangeSummary?> = local.observeExchange(id)
    fun observeAssets(exchangeId: Long): Flow<List<ExchangeAsset>> = local.observeAssets(exchangeId)
    fun observeMarkets(coinId: Long): Flow<List<CoinExchangeMarket>> = local.observeMarkets(coinId)
    fun observeHistory(coinId: Long): Flow<List<CoinHistoryPoint>> = local.observeHistory(coinId)
    fun coinDescription(id: Long): Pair<String?, String?> = local.coinDescription(id)

    fun coinCount(): Long = local.countCoins()
    fun exchangeCount(): Long = local.countExchanges()

    suspend fun refreshExchangeAssets(exchangeId: Long, force: Boolean = false): CryptoError? {
        val key = "exchange_assets:$exchangeId"
        if (!force && local.isFresh(key, config.metadataCacheTtlMillis)) return null
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
        val history = async { refreshHistory(coinId) }
        CoinDetailRefreshResult(
            quoteError = quote.await(),
            marketsError = markets.await(),
            historyError = history.await(),
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
        if (!force && local.isFresh(key, config.marketPairsCacheTtlMillis)) return null
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

    suspend fun refreshHistory(coinId: Long, force: Boolean = false): CryptoError? {
        val key = "coin_history:$coinId"
        if (!force && local.isFresh(key, config.historyCacheTtlMillis)) return null
        return when (val result = remote.coinHistory(coinId).first()) {
            is ApiResult.Success -> {
                local.replaceHistory(coinId, result.data)
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
}
