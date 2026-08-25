package br.com.rmf.kmp.cryptoview.domain.usecase

import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRequestExecutor
import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapService
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.utils.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetKeyInfoUseCase internal constructor(
    private val service: CoinMarketCapService,
    private val requestExecutor: CoinMarketCapRequestExecutor,
) : AbstractUseCase<ApiResult<CoinMarketCapKeyInfo>, GetKeyInfoUseCase.Params>() {

    override fun buildUseCaseFlow(
        params: Params,
    ): Flow<ApiResult<CoinMarketCapKeyInfo>> {
        if (!params.isValid()) {
            return flowOf(ApiResult.Failure(CryptoError.MissingApiKey))
        }

        return requestExecutor.execute { service.getKeyInfo(params.apiKey) }
            .map { result ->
                when (result) {
                    is ApiResult.Failure -> result
                    is ApiResult.Success -> ApiResult.Success(
                        data = result.data.toDomain(),
                        metadata = result.metadata,
                    )
                }
            }
    }

    data class Params(val apiKey: String) {
        fun isValid(): Boolean = apiKey.isNotBlank()
    }
}
