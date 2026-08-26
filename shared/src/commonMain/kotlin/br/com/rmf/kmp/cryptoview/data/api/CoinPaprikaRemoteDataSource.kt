package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaCoinDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaCurrencyDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class CoinPaprikaRemoteDataSource(
    private val service: CoinPaprikaService,
    private val requestExecutor: CoinPaprikaRequestExecutor,
    private val rateLimiter: ApiRateLimiter,
) {
    fun searchCoins(symbol: String): Flow<ApiResult<List<CoinPaprikaCurrencyDto>>> = limited {
        requestExecutor.execute { service.searchCoins(symbol) }.map { result ->
            when (result) {
                is ApiResult.Failure -> result
                is ApiResult.Success -> ApiResult.Success(result.data.currencies, result.metadata)
            }
        }
    }

    fun coinInformation(coinId: String): Flow<ApiResult<CoinPaprikaCoinDto>> = limited {
        requestExecutor.execute { service.getCoinById(coinId) }
    }

    private fun <T> limited(request: () -> Flow<ApiResult<T>>): Flow<ApiResult<T>> = flow {
        rateLimiter.acquire(COIN_PAPRIKA_REQUESTS_PER_MINUTE)
        emitAll(request())
    }

    private companion object {
        const val COIN_PAPRIKA_REQUESTS_PER_MINUTE = 600
    }
}
