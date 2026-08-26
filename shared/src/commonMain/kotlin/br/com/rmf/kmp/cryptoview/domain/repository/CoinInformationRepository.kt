package br.com.rmf.kmp.cryptoview.domain.repository

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import br.com.rmf.kmp.cryptoview.data.api.CoinPaprikaRemoteDataSource
import br.com.rmf.kmp.cryptoview.data.database.MarketLocalDataSource
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinInformation
import br.com.rmf.kmp.cryptoview.domain.model.CoinInformationFailure
import br.com.rmf.kmp.cryptoview.domain.model.CoinPaprikaIdResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinPaprikaMapping
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaCoinDto
import br.com.rmf.kmp.cryptoview.domain.model.api.CoinPaprikaCurrencyDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CoinInformationRepository internal constructor(
    private val local: MarketLocalDataSource,
    private val remote: CoinPaprikaRemoteDataSource,
) {
    private val resolutionMutex = Mutex()

    fun observeMapping(coinId: Long): Flow<CoinPaprikaMapping?> =
        local.observeCoinPaprikaMapping(coinId)

    fun observeInformation(coinId: Long): Flow<CoinInformation?> =
        local.observeCoinInformation(coinId)

    suspend fun resolveCoinId(
        coin: CoinSummary,
        force: Boolean = false,
    ): CoinPaprikaIdResult = resolutionMutex.withLock {
        if (!force) {
            local.coinPaprikaMapping(coin.id)?.let {
                return@withLock CoinPaprikaIdResult.Resolved(it.paprikaId)
            }
        }

        when (val result = remote.searchCoins(coin.symbol).first()) {
            is ApiResult.Failure -> CoinPaprikaIdResult.Failure(
                CoinInformationFailure.Request(result.error),
            )
            is ApiResult.Success -> {
                val candidate = selectCoinPaprikaCandidate(coin, result.data)
                    ?: return@withLock CoinPaprikaIdResult.Failure(
                        CoinInformationFailure.UnresolvedIdentity,
                    )
                local.replaceCoinPaprikaMapping(coin.id, candidate.id)
                CoinPaprikaIdResult.Resolved(candidate.id)
            }
        }
    }

    suspend fun refreshInformation(
        coin: CoinSummary,
        paprikaId: String,
        force: Boolean = false,
    ): CoinInformationFailure? {
        val mapping = local.coinPaprikaMapping(coin.id)
        if (mapping?.paprikaId != paprikaId) return CoinInformationFailure.UnresolvedIdentity

        val cached = local.coinInformation(coin.id)
        if (!force && cached?.paprikaId == paprikaId && cached.isFresh()) return null

        return when (val result = remote.coinInformation(paprikaId).first()) {
            is ApiResult.Failure -> {
                if (result.error is CryptoError.NotFound) {
                    local.invalidateCoinPaprikaMapping(coin.id)
                    CoinInformationFailure.UnresolvedIdentity
                } else {
                    CoinInformationFailure.Request(result.error)
                }
            }
            is ApiResult.Success -> {
                val dto = result.data
                if (!dto.matches(coin, paprikaId)) {
                    local.invalidateCoinPaprikaMapping(coin.id)
                    CoinInformationFailure.UnresolvedIdentity
                } else {
                    local.replaceCoinInformation(dto.toDomain(coin.id))
                    null
                }
            }
        }
    }

    private fun CoinInformation.isFresh(): Boolean =
        currentTimeMillis() - fetchedAt < DETAILS_CACHE_TTL_MILLIS

    private companion object {
        const val DETAILS_CACHE_TTL_MILLIS = 24 * 60 * 60_000L
    }
}

internal fun selectCoinPaprikaCandidate(
    coin: CoinSummary,
    candidates: List<CoinPaprikaCurrencyDto>,
): CoinPaprikaCurrencyDto? {
    val strictMatches = candidates.filter { candidate ->
        candidate.isActive == true &&
            candidate.symbol.equals(coin.symbol, ignoreCase = true) &&
            candidate.matchesLocalIdentity(coin)
    }
    return strictMatches.singleOrNull()
}

private fun CoinPaprikaCurrencyDto.matchesLocalIdentity(coin: CoinSummary): Boolean =
    canonicalIdentity(name) == canonicalIdentity(coin.name) ||
        idSlugMatches(id, coin.slug)

private fun CoinPaprikaCoinDto.matches(coin: CoinSummary, expectedId: String): Boolean =
    id == expectedId &&
        symbol.equals(coin.symbol, ignoreCase = true) &&
        (canonicalIdentity(name) == canonicalIdentity(coin.name) || idSlugMatches(id, coin.slug))

private fun CoinPaprikaCoinDto.toDomain(coinId: Long) = CoinInformation(
    coinId = coinId,
    paprikaId = id,
    name = name,
    symbol = symbol,
    rank = rank,
    isActive = isActive == true,
    type = type.cleanText(),
    logoUrl = logo.validHttpUrl(),
    description = description.cleanText(),
    message = message.cleanText(),
    openSource = openSource,
    hardwareWallet = hardwareWallet,
    startedAt = startedAt.cleanText(),
    developmentStatus = developmentStatus.cleanText(),
    proofType = proofType.cleanText(),
    orgStructure = orgStructure.cleanText(),
    hashAlgorithm = hashAlgorithm.cleanText(),
    websiteUrl = links?.website.firstValidHttpUrl(),
    explorerUrl = links?.explorer.firstValidHttpUrl(),
    sourceCodeUrl = links?.sourceCode.firstValidHttpUrl(),
    whitepaperUrl = whitepaper?.link.validHttpUrl(),
    fetchedAt = currentTimeMillis(),
)

private fun idSlugMatches(paprikaId: String, localSlug: String): Boolean {
    if (localSlug.isBlank() || '-' !in paprikaId) return false
    return canonicalIdentity(paprikaId.substringAfter('-')) == canonicalIdentity(localSlug)
}

private fun canonicalIdentity(value: String): String = value
    .trim()
    .lowercase()
    .filter(Char::isLetterOrDigit)

private fun String?.cleanText(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.validHttpUrl(): String? = this?.trim()?.takeIf { value ->
    value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("http://", ignoreCase = true)
}

private fun List<String>?.firstValidHttpUrl(): String? =
    this.orEmpty().firstNotNullOfOrNull(String::validHttpUrl)
