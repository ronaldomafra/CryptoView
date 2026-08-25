package br.com.rmf.kmp.cryptoview.utils

import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapCreditUsage
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapMinuteUsage
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapPlan
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapUsage
import br.com.rmf.kmp.cryptoview.domain.model.api.CreditUsageDto
import br.com.rmf.kmp.cryptoview.domain.model.api.KeyInfoDto

internal fun KeyInfoDto.toDomain(): CoinMarketCapKeyInfo = CoinMarketCapKeyInfo(
    plan = plan?.let {
        CoinMarketCapPlan(
            creditLimitMonthly = it.creditLimitMonthly,
            creditLimitMonthlyReset = it.creditLimitMonthlyReset,
            creditLimitMonthlyResetTimestamp = it.creditLimitMonthlyResetTimestamp,
            rateLimitMinute = it.rateLimitMinute,
        )
    },
    usage = usage?.let {
        CoinMarketCapUsage(
            currentMinute = it.currentMinute?.let { minute ->
                CoinMarketCapMinuteUsage(
                    requestsMade = minute.requestsMade,
                    requestsLeft = minute.requestsLeft,
                )
            },
            currentDay = it.currentDay?.toDomain(),
            currentMonth = it.currentMonth?.toDomain(),
        )
    },
)

private fun CreditUsageDto.toDomain() = CoinMarketCapCreditUsage(
    creditsUsed = creditsUsed,
    creditsLeft = creditsLeft,
)
