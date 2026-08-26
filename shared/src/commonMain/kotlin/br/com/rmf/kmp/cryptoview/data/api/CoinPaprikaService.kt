package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaCoinDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaSearchResponseDto
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import kotlinx.coroutines.flow.Flow

internal interface CoinPaprikaService {
    @GET("search")
    fun searchCoins(
        @Query("q") query: String,
        @Query("c") categories: String = "currencies",
        @Query("modifier") modifier: String = "symbol_search",
        @Query("limit") limit: Int = 250,
    ): Flow<Response<CoinPaprikaSearchResponseDto>>

    @GET("coins/{coinId}")
    fun getCoinById(
        @Path("coinId") coinId: String,
    ): Flow<Response<CoinPaprikaCoinDto>>
}
