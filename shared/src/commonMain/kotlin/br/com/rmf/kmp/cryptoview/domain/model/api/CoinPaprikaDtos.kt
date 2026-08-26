package br.com.rmf.kmp.cryptoview.domain.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CoinPaprikaSearchResponseDto(
    val currencies: List<CoinPaprikaCurrencyDto> = emptyList(),
)

@Serializable
internal data class CoinPaprikaCurrencyDto(
    val id: String,
    val name: String,
    val symbol: String,
    val rank: Long? = null,
    @SerialName("is_new") val isNew: Boolean? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    val type: String? = null,
)

@Serializable
internal data class CoinPaprikaCoinDto(
    val id: String,
    val name: String,
    val symbol: String,
    val rank: Long? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    val type: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val message: String? = null,
    @SerialName("open_source") val openSource: Boolean? = null,
    @SerialName("hardware_wallet") val hardwareWallet: Boolean? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("development_status") val developmentStatus: String? = null,
    @SerialName("proof_type") val proofType: String? = null,
    @SerialName("org_structure") val orgStructure: String? = null,
    @SerialName("hash_algorithm") val hashAlgorithm: String? = null,
    val links: CoinPaprikaLinksDto? = null,
    val whitepaper: CoinPaprikaWhitepaperDto? = null,
)

@Serializable
internal data class CoinPaprikaLinksDto(
    val explorer: List<String>? = null,
    val website: List<String>? = null,
    @SerialName("source_code") val sourceCode: List<String>? = null,
)

@Serializable
internal data class CoinPaprikaWhitepaperDto(
    val link: String? = null,
)

@Serializable
internal data class CoinPaprikaErrorDto(
    val error: String? = null,
)
