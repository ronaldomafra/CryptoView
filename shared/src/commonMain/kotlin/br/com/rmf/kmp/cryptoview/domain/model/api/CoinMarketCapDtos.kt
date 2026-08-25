package br.com.rmf.kmp.cryptoview.domain.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
