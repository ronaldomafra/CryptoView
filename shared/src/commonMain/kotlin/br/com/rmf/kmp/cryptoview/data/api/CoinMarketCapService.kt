package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.api.ApiEnvelope
import br.com.rmf.kmp.cryptoview.domain.model.api.KeyInfoDto
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import kotlinx.coroutines.flow.Flow

internal interface CoinMarketCapService {
    @GET("v1/key/info")
    fun getKeyInfo(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
    ): Flow<Response<ApiEnvelope<KeyInfoDto>>>
}
