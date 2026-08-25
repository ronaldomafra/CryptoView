package br.com.rmf.kmp.cryptoview.domain.model

data class CoinMarketCapKeyInfo(
    val plan: CoinMarketCapPlan?,
    val usage: CoinMarketCapUsage?,
)

data class CoinMarketCapPlan(
    val creditLimitMonthly: Int?,
    val creditLimitMonthlyReset: String?,
    val creditLimitMonthlyResetTimestamp: String?,
    val rateLimitMinute: Int?,
)

data class CoinMarketCapUsage(
    val currentMinute: CoinMarketCapMinuteUsage?,
    val currentDay: CoinMarketCapCreditUsage?,
    val currentMonth: CoinMarketCapCreditUsage?,
)

data class CoinMarketCapMinuteUsage(
    val requestsMade: Int?,
    val requestsLeft: Int?,
)

data class CoinMarketCapCreditUsage(
    val creditsUsed: Int?,
    val creditsLeft: Int?,
)
