package br.com.rmf.kmp.cryptoview.domain.model

enum class CoinSortOrder {
    RANK,
    MARKET_CAP,
    PRICE,
}

enum class CoinVariationFilter {
    ALL,
    POSITIVE,
    NEGATIVE,
}

enum class CoinHistoryRange(
    val label: String,
    val interval: String,
    val count: Int,
    val metricSuffix: String,
) {
    HOURS_24("24H", "1h", 24, "24h"),
    DAYS_7("7D", "2h", 84, "7d"),
    DAYS_30("30D", "12h", 60, "30d"),
    YEAR_1("1A", "7d", 52, "1a"),
    ;

    init {
        require(count in 1..100) { "O histórico deve consumir no máximo um crédito por consulta" }
    }
}

data class CoinSummary(
    val id: Long,
    val name: String,
    val symbol: String,
    val slug: String,
    val rank: Long?,
    val numMarketPairs: Long?,
    val priceUsd: Double?,
    val volume24hUsd: Double?,
    val percentChange24h: Double?,
    val marketCapUsd: Double?,
    val quoteUpdatedAt: String?,
    val quoteFetchedAt: Long?,
    val logoUrl: String?,
)

data class CoinDetail(
    val coin: CoinSummary,
    val description: String?,
    val websiteUrl: String?,
    val markets: List<CoinExchangeMarket>,
    val history: List<CoinHistoryPoint>,
)

data class CoinExchangeMarket(
    val coinId: Long,
    val exchangeId: Long,
    val exchangeName: String,
    val exchangeLogoUrl: String?,
    val marketPair: String,
    val category: String?,
    val priceUsd: Double?,
    val volume24hUsd: Double?,
    val updatedAt: String?,
)

data class CoinHistoryPoint(
    val timestamp: String,
    val priceUsd: Double,
)

data class ExchangeSummary(
    val id: Long,
    val name: String,
    val slug: String,
    val rank: Long?,
    val numMarketPairs: Long?,
    val spotVolumeUsd: Double?,
    val dateLaunched: String?,
    val logoUrl: String?,
    val description: String?,
    val websiteUrl: String?,
    val makerFee: Double?,
    val takerFee: Double?,
)

data class ExchangeAsset(
    val exchangeId: Long,
    val currencyId: Long,
    val name: String,
    val symbol: String,
    val priceUsd: Double?,
    val balance: Double?,
    val valueUsd: Double?,
)

data class ExchangeDetail(
    val exchange: ExchangeSummary,
    val assets: List<ExchangeAsset>,
)

data class ApiQuotaSnapshot(
    val monthlyLimit: Int?,
    val monthlyUsed: Int?,
    val monthlyLeft: Int?,
    val requestsPerMinute: Int?,
) {
    fun isInReserve(reservePercent: Int): Boolean {
        val limit = monthlyLimit ?: return false
        val left = monthlyLeft ?: return false
        return left <= (limit * reservePercent / 100)
    }
}
