package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryRange
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinHistoryDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinListingDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinMarketPairsDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinMetadataDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeAssetDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeListingDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeMetadataDto
import br.com.rmf.kmp.cryptoview.domain.model.api.GlobalMetricsDto
import br.com.rmf.kmp.cryptoview.domain.model.api.KeyInfoDto
import br.com.rmf.kmp.cryptoview.utils.SyncPerformanceTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class CoinMarketCapRemoteDataSource(
    private val service: CoinMarketCapService,
    private val requestExecutor: CoinMarketCapRequestExecutor,
    private val authenticatedExecutor: AuthenticatedRequestExecutor,
    private val rateLimiter: ApiRateLimiter,
    private val performanceTracker: SyncPerformanceTracker = SyncPerformanceTracker(),
) {
    private var requestsPerMinute = FALLBACK_REQUESTS_PER_MINUTE

    fun updateRateLimit(limit: Int?) {
        requestsPerMinute = limit?.coerceAtMost(FALLBACK_REQUESTS_PER_MINUTE)
            ?: FALLBACK_REQUESTS_PER_MINUTE
    }

    fun keyInfo(): Flow<ApiResult<KeyInfoDto>> = authenticated("key_info") {
        requestExecutor.execute { service.getKeyInfo(it) }
    }

    fun globalMetrics(): Flow<ApiResult<GlobalMetricsDto>> = authenticated("global_metrics") {
        requestExecutor.execute { service.getGlobalMetrics(it) }
    }

    fun coinListings(start: Int, limit: Int): Flow<ApiResult<List<CoinListingDto>>> = authenticated("coin_listings") {
        requestExecutor.execute { service.getCoinListings(it, start, limit) }
    }

    fun coinMetadata(ids: List<Long>): Flow<ApiResult<Map<String, CoinMetadataDto>>> = authenticated("coin_metadata") {
        requestExecutor.execute { service.getCoinMetadata(it, ids.joinToString(",")) }
    }

    fun coinQuotes(ids: List<Long>): Flow<ApiResult<Map<String, CoinListingDto>>> = authenticated("coin_quotes") {
        requestExecutor.execute { service.getCoinQuotes(it, ids.joinToString(",")) }
    }

    fun coinHistory(id: Long, range: CoinHistoryRange): Flow<ApiResult<CoinHistoryDto>> =
        authenticatedWithoutRateLimit("coin_history") { apiKey ->
            requestExecutor.execute {
                service.getCoinHistory(
                    apiKey = apiKey,
                    id = id,
                    interval = range.interval,
                    count = range.count,
                )
            }.map { result ->
                when (result) {
                    is ApiResult.Failure -> result
                    is ApiResult.Success -> {
                        val history = result.data.coinHistoryDto(id)
                        val hasPricePoints = history?.quotes.orEmpty().any { quote ->
                            quote.timestamp != null && quote.quote.usdQuote().doubleValue("price") != null
                        }
                        if (!hasPricePoints) {
                            ApiResult.Failure(CryptoError.InvalidResponse("Histórico da moeda ausente"))
                        } else {
                            ApiResult.Success(requireNotNull(history), result.metadata)
                        }
                    }
                }
            }
        }

    fun coinMarketPairs(id: Long): Flow<ApiResult<CoinMarketPairsDto>> = authenticated("coin_market_pairs") {
        requestExecutor.execute { service.getCoinMarketPairs(it, id) }
    }

    fun exchangeListings(start: Int, limit: Int): Flow<ApiResult<List<ExchangeListingDto>>> = authenticated("exchange_listings") {
        requestExecutor.execute { service.getExchangeListings(it, start, limit) }
    }

    fun exchangeMetadata(ids: List<Long>): Flow<ApiResult<Map<String, ExchangeMetadataDto>>> = authenticated("exchange_metadata") {
        requestExecutor.execute { service.getExchangeMetadata(it, ids.joinToString(",")) }
    }

    fun exchangeAssets(id: Long): Flow<ApiResult<List<ExchangeAssetDto>>> = authenticated("exchange_assets") {
        requestExecutor.execute { service.getExchangeAssets(it, id) }
    }

    private fun <T> authenticated(
        operation: String,
        request: (String) -> Flow<ApiResult<T>>,
    ): Flow<ApiResult<T>> = authenticatedExecutor.execute { apiKey ->
        flow {
            val waitMillis = rateLimiter.acquire(requestsPerMinute)
            performanceTracker.record("rate_limit.$operation", waitMillis = waitMillis)
            val startedAt = performanceTracker.mark()
            try {
                emitAll(request(apiKey))
            } finally {
                performanceTracker.recordElapsed("network.$operation", startedAt)
            }
        }
    }

    private fun <T> authenticatedWithoutRateLimit(
        operation: String,
        request: (String) -> Flow<ApiResult<T>>,
    ): Flow<ApiResult<T>> = authenticatedExecutor.execute { apiKey ->
        flow {
            val startedAt = performanceTracker.mark()
            try {
                emitAll(request(apiKey))
            } finally {
                performanceTracker.recordElapsed("network.$operation", startedAt)
            }
        }
    }

    private companion object {
        const val FALLBACK_REQUESTS_PER_MINUTE = 45
    }
}
