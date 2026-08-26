package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinHistoryDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinListingDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinMarketPairsDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinMetadataDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeAssetDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeListingDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeMetadataDto
import br.com.rmf.kmp.cryptoview.domain.model.api.GlobalMetricsDto
import br.com.rmf.kmp.cryptoview.domain.model.api.KeyInfoDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class CoinMarketCapRemoteDataSource(
    private val service: CoinMarketCapService,
    private val requestExecutor: CoinMarketCapRequestExecutor,
    private val authenticatedExecutor: AuthenticatedRequestExecutor,
    private val rateLimiter: ApiRateLimiter,
) {
    private var requestsPerMinute = FALLBACK_REQUESTS_PER_MINUTE

    fun updateRateLimit(limit: Int?) {
        requestsPerMinute = limit?.coerceAtMost(FALLBACK_REQUESTS_PER_MINUTE)
            ?: FALLBACK_REQUESTS_PER_MINUTE
    }

    fun keyInfo(): Flow<ApiResult<KeyInfoDto>> = authenticated {
        requestExecutor.execute { service.getKeyInfo(it) }
    }

    fun globalMetrics(): Flow<ApiResult<GlobalMetricsDto>> = authenticated {
        requestExecutor.execute { service.getGlobalMetrics(it) }
    }

    fun coinListings(start: Int, limit: Int): Flow<ApiResult<List<CoinListingDto>>> = authenticated {
        requestExecutor.execute { service.getCoinListings(it, start, limit) }
    }

    fun coinMetadata(ids: List<Long>): Flow<ApiResult<Map<String, CoinMetadataDto>>> = authenticated {
        requestExecutor.execute { service.getCoinMetadata(it, ids.joinToString(",")) }
    }

    fun coinQuotes(ids: List<Long>): Flow<ApiResult<Map<String, CoinListingDto>>> = authenticated {
        requestExecutor.execute { service.getCoinQuotes(it, ids.joinToString(",")) }
    }

    fun coinHistory(id: Long): Flow<ApiResult<CoinHistoryDto>> = authenticated {
        requestExecutor.execute { service.getCoinHistory(it, id) }
    }

    fun coinMarketPairs(id: Long): Flow<ApiResult<CoinMarketPairsDto>> = authenticated {
        requestExecutor.execute { service.getCoinMarketPairs(it, id) }
    }

    fun exchangeListings(start: Int, limit: Int): Flow<ApiResult<List<ExchangeListingDto>>> = authenticated {
        requestExecutor.execute { service.getExchangeListings(it, start, limit) }
    }

    fun exchangeMetadata(ids: List<Long>): Flow<ApiResult<Map<String, ExchangeMetadataDto>>> = authenticated {
        requestExecutor.execute { service.getExchangeMetadata(it, ids.joinToString(",")) }
    }

    fun exchangeAssets(id: Long): Flow<ApiResult<List<ExchangeAssetDto>>> = authenticated {
        requestExecutor.execute { service.getExchangeAssets(it, id) }
    }

    private fun <T> authenticated(
        request: (String) -> Flow<ApiResult<T>>,
    ): Flow<ApiResult<T>> = authenticatedExecutor.execute { apiKey ->
        flow {
            rateLimiter.acquire(requestsPerMinute)
            emitAll(request(apiKey))
        }
    }

    private companion object {
        const val FALLBACK_REQUESTS_PER_MINUTE = 45
    }
}
