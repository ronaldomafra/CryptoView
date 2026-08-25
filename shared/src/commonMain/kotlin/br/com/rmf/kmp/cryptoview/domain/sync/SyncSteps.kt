package br.com.rmf.kmp.cryptoview.domain.sync

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRemoteDataSource
import br.com.rmf.kmp.cryptoview.data.database.MarketLocalDataSource
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.SyncContext
import br.com.rmf.kmp.cryptoview.domain.model.SyncEvent
import br.com.rmf.kmp.cryptoview.domain.model.SyncPhase
import br.com.rmf.kmp.cryptoview.domain.model.SyncStepDecision
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class RemoteSyncStep(
    protected val remote: CoinMarketCapRemoteDataSource,
    protected val local: MarketLocalDataSource,
    protected val config: CryptoProcessConfig,
) : SyncStep {
    protected suspend fun <T> requestWithRetry(request: () -> Flow<ApiResult<T>>): T {
        var attempt = 0
        while (true) {
            when (val result = request().first()) {
                is ApiResult.Success -> return result.data
                is ApiResult.Failure -> {
                    val transient = result.error is CryptoError.Timeout ||
                        result.error is CryptoError.NoConnection ||
                        result.error is CryptoError.ServerUnavailable ||
                        result.error is CryptoError.RateLimited
                    if (!transient || attempt >= config.maxRetryAttempts) {
                        throw SyncStepException(result.error)
                    }
                    val retryDelay = (result.error as? CryptoError.RateLimited)?.retryAfterMillis
                        ?: (1_000L shl attempt.coerceAtMost(4))
                    attempt += 1
                    delay(retryDelay)
                }
            }
        }
    }
}

internal class SyncExchangesStep(
    remote: CoinMarketCapRemoteDataSource,
    local: MarketLocalDataSource,
    config: CryptoProcessConfig,
) : RemoteSyncStep(remote, local, config) {
    override val phase = SyncPhase.EXCHANGES

    override suspend fun validate(context: SyncContext): SyncStepDecision {
        if (context.trigger != SyncTrigger.STARTUP_ESSENTIAL) return SyncStepDecision.Run
        val last = local.resourceLastSuccess(RESOURCE_KEY) ?: return SyncStepDecision.Run
        return if (currentTimeMillis() - last < config.exchangeCacheTtlMillis) {
            SyncStepDecision.Skip("Corretoras ainda estão atualizadas")
        } else SyncStepDecision.Run
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(context: SyncContext): Flow<SyncEvent> = flow {
        emit(SyncEvent.PhaseStarted(phase, "Baixando corretoras"))
        val pageSize = config.networkPageSize
        var firstPage = (context.committedPages[phase]?.maxOrNull() ?: 0) + 1
        var reachedLastPage = false
        var discovered = local.countExchanges()

        while (!reachedLastPage) {
            val pages = (firstPage until firstPage + EXCHANGE_PAGE_WINDOW).toList()
            val pageResults = mutableListOf<Pair<Int, Int>>()
            val pageResultsMutex = Mutex()
            pages.asFlow()
                .flatMapMerge(concurrency = EXCHANGE_PAGE_WINDOW) { page ->
                    flow {
                        emit(SyncEvent.PageRequested(phase, page))
                        val items = requestWithRetry {
                            remote.exchangeListings(start = ((page - 1) * pageSize) + 1, limit = pageSize)
                        }
                        local.persistExchangePage(context.runId, page, items)
                        pageResultsMutex.withLock { pageResults += page to items.size }
                        emit(SyncEvent.PageCommitted(phase, page, items.size))
                    }
                }
                .buffer(config.responseBufferCapacity)
                .collect { emit(it) }

            reachedLastPage = pageResults.any { it.second < pageSize }
            firstPage += EXCHANGE_PAGE_WINDOW
            if (pageResults.all { it.second == 0 }) reachedLastPage = true
        }

        discovered = local.countExchanges()
        local.markExchangesComplete(context.runId)
        local.markResourceSuccess(RESOURCE_KEY)
        emit(SyncEvent.TargetDiscovered(phase, discovered))
    }

    private companion object {
        const val EXCHANGE_PAGE_WINDOW = 4
        const val RESOURCE_KEY = "exchange_catalog"
    }
}

internal class SyncExchangeMetadataStep(
    remote: CoinMarketCapRemoteDataSource,
    local: MarketLocalDataSource,
    config: CryptoProcessConfig,
) : RemoteSyncStep(remote, local, config) {
    override val phase = SyncPhase.EXCHANGE_METADATA

    override fun execute(context: SyncContext): Flow<SyncEvent> = flow {
        emit(SyncEvent.PhaseStarted(phase, "Atualizando corretoras"))
        var page = 1
        var skippedMissingRows = 0L
        val minimum = currentTimeMillis() - config.metadataCacheTtlMillis
        while (true) {
            val ids = local.exchangeIdsMissingMetadata(
                minimum,
                config.networkPageSize.toLong(),
                skippedMissingRows,
            )
            if (ids.isEmpty()) break
            emit(SyncEvent.PageRequested(phase, page))
            val items = requestWithRetry { remote.exchangeMetadata(ids) }
            local.persistExchangeMetadata(context.runId, page, items)
            emit(SyncEvent.PageCommitted(phase, page, items.size))
            skippedMissingRows += (ids.size - items.size).coerceAtLeast(0)
            page += 1
        }
    }
}

internal class SyncCoinsStep(
    remote: CoinMarketCapRemoteDataSource,
    local: MarketLocalDataSource,
    config: CryptoProcessConfig,
) : RemoteSyncStep(remote, local, config) {
    override val phase = SyncPhase.COINS

    override suspend fun validate(context: SyncContext): SyncStepDecision {
        if (context.quota.isInReserve(config.quotaReservePercent)) {
            return SyncStepDecision.Skip("Cota reservada para os dados essenciais")
        }
        return if (context.trigger == SyncTrigger.STARTUP_ESSENTIAL && local.countCoins() > 0) {
            SyncStepDecision.Skip("Catálogo completo é atualizado manualmente")
        } else SyncStepDecision.Run
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(context: SyncContext): Flow<SyncEvent> = flow {
        emit(SyncEvent.PhaseStarted(phase, "Baixando moedas"))
        val total = runCatching {
            requestWithRetry { remote.globalMetrics() }.totalCryptocurrencies
        }.getOrNull()
        if (total != null) emit(SyncEvent.TargetDiscovered(phase, total.toLong()))

        val pageSize = config.networkPageSize
        var firstPage = (context.committedPages[phase]?.maxOrNull() ?: 0) + 1
        var reachedLastPage = false
        while (!reachedLastPage) {
            val pages = (firstPage until firstPage + config.networkParallelism).toList()
            val pageResults = mutableListOf<Pair<Int, Int>>()
            val pageResultsMutex = Mutex()
            pages.asFlow()
                .flatMapMerge(concurrency = config.networkParallelism) { page ->
                    flow {
                        emit(SyncEvent.PageRequested(phase, page))
                        val items = requestWithRetry {
                            remote.coinListings(((page - 1) * pageSize) + 1, pageSize)
                        }
                        local.persistCoinPage(context.runId, page, items)
                        pageResultsMutex.withLock { pageResults += page to items.size }
                        emit(SyncEvent.PageCommitted(phase, page, items.size))
                    }
                }
                .buffer(config.responseBufferCapacity)
                .collect { emit(it) }

            reachedLastPage = pageResults.any { it.second < pageSize } || pageResults.all { it.second == 0 }
            firstPage += config.networkParallelism
        }
        local.markCoinsComplete(context.runId)
        local.markResourceSuccess("coin_catalog")
        emit(SyncEvent.TargetDiscovered(phase, local.countCoins()))
    }
}

internal class SyncCoinMetadataStep(
    remote: CoinMarketCapRemoteDataSource,
    local: MarketLocalDataSource,
    config: CryptoProcessConfig,
) : RemoteSyncStep(remote, local, config) {
    override val phase = SyncPhase.COIN_METADATA

    override suspend fun validate(context: SyncContext): SyncStepDecision =
        if (context.quota.isInReserve(config.quotaReservePercent)) {
            SyncStepDecision.Skip("Metadados adiados para preservar a cota")
        } else if (context.trigger == SyncTrigger.STARTUP_ESSENTIAL && local.countCoins() > 0) {
            SyncStepDecision.Skip("Metadados completos são atualizados manualmente")
        } else SyncStepDecision.Run

    override fun execute(context: SyncContext): Flow<SyncEvent> = flow {
        emit(SyncEvent.PhaseStarted(phase, "Atualizando moedas"))
        val minimum = currentTimeMillis() - config.metadataCacheTtlMillis
        var page = 1
        var skippedMissingRows = 0L
        while (true) {
            val ids = local.coinIdsMissingMetadata(
                minimum,
                config.networkPageSize.toLong(),
                skippedMissingRows,
            )
            if (ids.isEmpty()) break
            emit(SyncEvent.PageRequested(phase, page))
            val items = requestWithRetry { remote.coinMetadata(ids) }
            local.persistCoinMetadata(context.runId, page, items)
            emit(SyncEvent.PageCommitted(phase, page, items.size))
            skippedMissingRows += (ids.size - items.size).coerceAtLeast(0)
            page += 1
        }
    }
}
