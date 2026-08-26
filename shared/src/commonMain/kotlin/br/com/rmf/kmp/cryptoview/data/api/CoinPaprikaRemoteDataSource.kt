package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaCoinDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaCurrencyDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class CoinPaprikaRemoteDataSource(
    private val service: CoinPaprikaService,
    private val requestExecutor: CoinPaprikaRequestExecutor,
) {
    fun searchCoins(symbol: String): Flow<ApiResult<List<CoinPaprikaCurrencyDto>>> =
        requestExecutor.execute { service.searchCoins(symbol) }.map { result ->
            when (result) {
                is ApiResult.Failure -> result
                is ApiResult.Success -> ApiResult.Success(result.data.currencies, result.metadata)
            }
        }

    fun coinInformation(coinId: String): Flow<ApiResult<CoinPaprikaCoinDto>> =
        requestExecutor.execute { service.getCoinById(coinId) }
}
