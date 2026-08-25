package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.api.ApiEnvelope
import br.com.rmf.kmp.cryptoview.domain.model.api.KeyInfoDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinHistoryDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinListingDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinMarketPairsDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinMetadataDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeAssetDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeListingDto
import br.com.rmf.kmp.cryptoview.domain.model.api.ExchangeMetadataDto
import br.com.rmf.kmp.cryptoview.domain.model.api.GlobalMetricsDto
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Query
import kotlinx.coroutines.flow.Flow

internal interface CoinMarketCapService {
    @GET("v1/key/info")
    fun getKeyInfo(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
    ): Flow<Response<ApiEnvelope<KeyInfoDto>>>

    @GET("v1/global-metrics/quotes/latest")
    fun getGlobalMetrics(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
    ): Flow<Response<ApiEnvelope<GlobalMetricsDto>>>

    @GET("v3/cryptocurrency/listings/latest")
    fun getCoinListings(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
        @Query("start") start: Int,
        @Query("limit") limit: Int,
        @Query("convert") convert: String = "USD",
        @Query("sort") sort: String = "market_cap",
        @Query("sort_dir") sortDirection: String = "desc",
    ): Flow<Response<ApiEnvelope<List<CoinListingDto>>>>

    @GET("v2/cryptocurrency/info")
    fun getCoinMetadata(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
        @Query("id") ids: String,
        @Query("aux") aux: String = "urls,logo,description",
    ): Flow<Response<ApiEnvelope<Map<String, CoinMetadataDto>>>>

    @GET("v3/cryptocurrency/quotes/latest")
    fun getCoinQuotes(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
        @Query("id") ids: String,
        @Query("convert") convert: String = "USD",
    ): Flow<Response<ApiEnvelope<Map<String, CoinListingDto>>>>

    @GET("v3/cryptocurrency/quotes/historical")
    fun getCoinHistory(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
        @Query("id") id: Long,
        @Query("convert") convert: String = "USD",
        @Query("interval") interval: String = "1h",
        @Query("count") count: Int = 24,
    ): Flow<Response<ApiEnvelope<CoinHistoryDto>>>

    @GET("v2/cryptocurrency/market-pairs/latest")
    fun getCoinMarketPairs(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
        @Query("id") id: Long,
        @Query("start") start: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("category") category: String = "spot",
        @Query("convert") convert: String = "USD",
    ): Flow<Response<ApiEnvelope<CoinMarketPairsDto>>>

    @GET("v1/exchange/listings/latest")
    fun getExchangeListings(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
        @Query("start") start: Int,
        @Query("limit") limit: Int,
        @Query("convert") convert: String = "USD",
        @Query("sort") sort: String = "volume_24h",
        @Query("sort_dir") sortDirection: String = "desc",
        @Query("market_type") marketType: String = "all",
        @Query("aux") aux: String = "num_market_pairs,rank,date_launched",
    ): Flow<Response<ApiEnvelope<List<ExchangeListingDto>>>>

    @GET("v1/exchange/info")
    fun getExchangeMetadata(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
        @Query("id") ids: String,
        @Query("aux") aux: String = "urls,logo,description,date_launched,status",
    ): Flow<Response<ApiEnvelope<Map<String, ExchangeMetadataDto>>>>

    @GET("v1/exchange/assets")
    fun getExchangeAssets(
        @Header("X-CMC_PRO_API_KEY") apiKey: String,
        @Query("id") id: Long,
    ): Flow<Response<ApiEnvelope<List<ExchangeAssetDto>>>>
}
