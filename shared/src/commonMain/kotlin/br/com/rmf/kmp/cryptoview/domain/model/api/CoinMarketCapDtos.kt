package br.com.rmf.kmp.cryptoview.domain.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ApiEnvelope<T>(
    val data: T? = null,
    val status: ApiStatusDto,
)

@Serializable
internal data class ErrorEnvelope(
    val status: ApiStatusDto? = null,
)

@Serializable
internal data class ApiStatusDto(
    val timestamp: String? = null,
    @SerialName("error_code") val errorCode: Int? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    val elapsed: Int? = null,
    @SerialName("credit_count") val creditCount: Int? = null,
    val notice: String? = null,
)

@Serializable
internal data class KeyInfoDto(
    val plan: PlanDto? = null,
    val usage: UsageDto? = null,
)

@Serializable
internal data class PlanDto(
    @SerialName("credit_limit_monthly") val creditLimitMonthly: Int? = null,
    @SerialName("credit_limit_monthly_reset") val creditLimitMonthlyReset: String? = null,
    @SerialName("credit_limit_monthly_reset_timestamp")
    val creditLimitMonthlyResetTimestamp: String? = null,
    @SerialName("rate_limit_minute") val rateLimitMinute: Int? = null,
)

@Serializable
internal data class UsageDto(
    @SerialName("current_minute") val currentMinute: MinuteUsageDto? = null,
    @SerialName("current_day") val currentDay: CreditUsageDto? = null,
    @SerialName("current_month") val currentMonth: CreditUsageDto? = null,
)

@Serializable
internal data class MinuteUsageDto(
    @SerialName("requests_made") val requestsMade: Int? = null,
    @SerialName("requests_left") val requestsLeft: Int? = null,
)

@Serializable
internal data class CreditUsageDto(
    @SerialName("credits_used") val creditsUsed: Int? = null,
    @SerialName("credits_left") val creditsLeft: Int? = null,
)

@Serializable
internal data class GlobalMetricsDto(
    @SerialName("total_cryptocurrencies") val totalCryptocurrencies: Int? = null,
)

@Serializable
internal data class CoinListingDto(
    val id: Long,
    val name: String? = null,
    val symbol: String? = null,
    val slug: String? = null,
    @SerialName("cmc_rank") val rank: Long? = null,
    @SerialName("num_market_pairs") val numMarketPairs: Long? = null,
    @SerialName("circulating_supply") val circulatingSupply: Double? = null,
    @SerialName("total_supply") val totalSupply: Double? = null,
    @SerialName("max_supply") val maxSupply: Double? = null,
    @SerialName("date_added") val dateAdded: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
    val quote: JsonElement? = null,
)

@Serializable
internal data class CoinMetadataDto(
    val id: Long? = null,
    val name: String? = null,
    val symbol: String? = null,
    val slug: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val urls: ResourceUrlsDto? = null,
)

@Serializable
internal data class ResourceUrlsDto(
    val website: List<String>? = null,
)

@Serializable
internal data class ExchangeListingDto(
    val id: Long,
    val name: String? = null,
    val slug: String? = null,
    val rank: Long? = null,
    @SerialName("num_market_pairs") val numMarketPairs: Long? = null,
    @SerialName("date_launched") val dateLaunched: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
    val quote: JsonElement? = null,
)

@Serializable
internal data class ExchangeMetadataDto(
    val id: Long? = null,
    val name: String? = null,
    val slug: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val urls: ResourceUrlsDto? = null,
    @SerialName("date_launched") val dateLaunched: String? = null,
    @SerialName("maker_fee") val makerFee: Double? = null,
    @SerialName("taker_fee") val takerFee: Double? = null,
)

@Serializable
internal data class ExchangeAssetDto(
    @SerialName("wallet_address") val walletAddress: String? = null,
    val balance: Double? = null,
    val platform: JsonElement? = null,
    val currency: ExchangeAssetCurrencyDto? = null,
)

@Serializable
internal data class ExchangeAssetCurrencyDto(
    val id: Long? = null,
    val name: String? = null,
    val symbol: String? = null,
    @SerialName("price_usd") val priceUsd: Double? = null,
)

@Serializable
internal data class CoinMarketPairsDto(
    val id: Long? = null,
    val name: String? = null,
    val symbol: String? = null,
    @SerialName("num_market_pairs") val numMarketPairs: Long? = null,
    @SerialName("market_pairs") val marketPairs: List<CoinMarketPairDto>? = null,
)

@Serializable
internal data class CoinMarketPairDto(
    val exchange: MarketPairExchangeDto? = null,
    @SerialName("market_pair") val marketPair: String? = null,
    val category: String? = null,
    val quote: JsonElement? = null,
)

@Serializable
internal data class MarketPairExchangeDto(
    val id: Long? = null,
    val name: String? = null,
    val slug: String? = null,
)

@Serializable
internal data class CoinHistoryDto(
    val id: Long? = null,
    val name: String? = null,
    val symbol: String? = null,
    val quotes: List<CoinHistoryQuoteDto>? = null,
)

@Serializable
internal data class CoinHistoryQuoteDto(
    val timestamp: String? = null,
    val quote: JsonElement? = null,
)
