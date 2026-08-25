package br.com.rmf.kmp.cryptoview.domain.repository

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo
import br.com.rmf.kmp.cryptoview.domain.usecase.GetKeyInfoUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class CoinMarketCapDataRepository internal constructor(
    private val getKeyInfoUseCase: GetKeyInfoUseCase,
) {
    fun getKeyInfo(
        apiKey: String,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): Flow<ApiResult<CoinMarketCapKeyInfo>> = getKeyInfoUseCase.execute(
        params = GetKeyInfoUseCase.Params(apiKey),
        backgroundDispatcher = backgroundDispatcher,
    )
}
