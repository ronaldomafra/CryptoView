package br.com.rmf.kmp.cryptoview.domain.sync

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRemoteDataSource
import br.com.rmf.kmp.cryptoview.data.database.MarketLocalDataSource
import br.com.rmf.kmp.cryptoview.domain.model.ApiQuotaSnapshot
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.SyncPhase
import br.com.rmf.kmp.cryptoview.domain.model.SyncProgress
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStatus
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStorage
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import br.com.rmf.kmp.cryptoview.utils.mapParallel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal fun interface CryptoSyncCoordinator {
    suspend fun execute(
        trigger: SyncTrigger,
        onProgress: (SyncProgress) -> Unit,
    )
}

internal class DefaultCryptoSyncCoordinator(
    private val secureApiKeyStorage: SecureApiKeyStorage,
    private val remote: CoinMarketCapRemoteDataSource,
    private val local: MarketLocalDataSource,
    private val config: CryptoProcessConfig,
) : CryptoSyncCoordinator {

    override suspend fun execute(
        trigger: SyncTrigger,
        onProgress: (SyncProgress) -> Unit,
    ) {
        var latestProgress = SyncProgress()
        val report: (SyncProgress) -> Unit = { progress ->
            latestProgress = progress
            onProgress(progress)
        }

        try {
            if (secureApiKeyStorage.status() != SecureApiKeyStatus.CONFIGURED) {
                report(
                    SyncProgress(
                        status = SyncStatus.FAILED,
                        phase = SyncPhase.VALIDATING_CREDENTIAL,
                        error = CryptoError.MissingApiKey,
                        message = "Configure uma API key antes de sincronizar.",
                    ),
                )
                return
            }

            report(
                SyncProgress(
                    trigger = trigger,
                    phase = SyncPhase.VALIDATING_CREDENTIAL,
                    status = SyncStatus.RUNNING,
                    message = "Validando acesso",
                ),
            )

            val keyInfo = when (val result = remote.keyInfo().first()) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> {
                    report(
                        latestProgress.copy(
                            status = SyncStatus.FAILED,
                            error = result.error,
                            message = errorMessage(result.error),
                        ),
                    )
                    return
                }
            }
            remote.updateRateLimit(keyInfo.plan?.rateLimitMinute)

            val quota = ApiQuotaSnapshot(
                monthlyLimit = keyInfo.plan?.creditLimitMonthly,
                monthlyUsed = keyInfo.usage?.currentMonth?.creditsUsed,
                monthlyLeft = keyInfo.usage?.currentMonth?.creditsLeft,
                requestsPerMinute = keyInfo.plan?.rateLimitMinute,
            )
            val resumable = if (trigger == SyncTrigger.MANUAL_FULL) null else local.latestIncompleteRun()
            val runId = resumable?.runId ?: "${currentTimeMillis()}-${trigger.name.lowercase()}"
            val effectiveTrigger = resumable?.trigger ?: trigger
            val committedPages = SyncPhase.entries.associateWith { phase ->
                local.committedPages(runId, phase)
            }
            val initialProgress = SyncProgress(
                runId = runId,
                trigger = effectiveTrigger,
                phase = resumable?.phase ?: SyncPhase.PREPARING,
                status = SyncStatus.RUNNING,
                persistedItems = resumable?.persistedItems ?: 0,
                requestedPages = resumable?.requestedPages ?: 0,
                committedPages = resumable?.committedPages ?: 0,
                failedPages = resumable?.failedPages ?: 0,
                message = if (resumable == null) "Preparando sincronização" else "Retomando sincronização",
            )

            if (resumable == null) local.createRun(initialProgress) else local.updateRun(initialProgress)
            report(initialProgress)

            val session = RunSession(
                runId = runId,
                trigger = effectiveTrigger,
                quota = quota,
                committedPages = committedPages,
                initialProgress = initialProgress,
                report = report,
            )

            syncExchanges(session)
            syncExchangeMetadata(session)
            syncCoins(session)
            syncCoinMetadata(session)
            session.complete()
        } catch (exception: CancellationException) {
            withContext(NonCancellable) {
                publishFinal(
                    latestProgress.copy(
                        status = SyncStatus.PAUSED,
                        message = "Sincronização pausada; o progresso foi preservado.",
                    ),
                    report,
                )
            }
        } catch (exception: SyncFailure) {
            publishFinal(
                latestProgress.copy(
                    status = SyncStatus.FAILED,
                    failedPages = latestProgress.failedPages + 1,
                    error = exception.error,
                    message = errorMessage(exception.error),
                ),
                report,
            )
        } catch (exception: Throwable) {
            val error = CryptoError.Unknown(exception.message)
            publishFinal(
                latestProgress.copy(
                    status = SyncStatus.FAILED,
                    error = error,
                    message = errorMessage(error),
                ),
                report,
            )
        }
    }

    private suspend fun syncExchanges(session: RunSession) {
        val phase = SyncPhase.EXCHANGES
        if (session.trigger == SyncTrigger.STARTUP_ESSENTIAL) {
            val lastSuccess = local.resourceLastSuccess(EXCHANGE_RESOURCE_KEY)
            if (lastSuccess != null && currentTimeMillis() - lastSuccess < EXCHANGE_CACHE_TTL_MILLIS) {
                session.skip(phase, "Corretoras ainda estão atualizadas")
                return
            }
        }

        session.startPhase(phase, "Baixando corretoras")
        var firstPage = (session.committedPages.getValue(phase).maxOrNull() ?: 0) + 1
        var reachedLastPage = false
        var restrictedByPlan = false

        if (firstPage == 1) {
            session.requestPages(phase, 1)
            val items = try {
                requestWithRetry { remote.exchangeListings(start = 1, limit = NETWORK_PAGE_SIZE) }
            } catch (exception: SyncFailure) {
                if (exception.error is CryptoError.PlanUnavailable) {
                    local.markResourceFailure(EXCHANGE_RESOURCE_KEY, PLAN_UNAVAILABLE)
                    session.skip(
                        phase,
                        "Corretoras indisponíveis no plano atual",
                        restricted = true,
                    )
                    session.discoverTarget(phase, local.countExchanges())
                    return
                }
                throw exception
            }
            local.persistExchangePage(session.runId, firstPage, items)
            session.commitPage(phase, items.size)
            reachedLastPage = items.size < NETWORK_PAGE_SIZE
            firstPage += 1
        }

        while (!reachedLastPage) {
            val pages = (firstPage until firstPage + config.parallelIoValue).toList()
            val pageResults = processPageWindow(
                session = session,
                phase = phase,
                pages = pages,
                download = { page ->
                    remote.exchangeListings(
                        start = ((page - 1) * NETWORK_PAGE_SIZE) + 1,
                        limit = NETWORK_PAGE_SIZE,
                    )
                },
                persist = { page, items -> local.persistExchangePage(session.runId, page, items) },
            )
            restrictedByPlan = restrictedByPlan || pageResults.any(PageCommit::restricted)

            reachedLastPage = restrictedByPlan || pageResults.any { !it.restricted && it.items < NETWORK_PAGE_SIZE }
            firstPage += config.parallelIoValue
        }

        if (!restrictedByPlan) local.markExchangesComplete(session.runId)
        local.markResourceSuccess(EXCHANGE_RESOURCE_KEY)
        if (restrictedByPlan) {
            session.skip(phase, "Catálogo de corretoras limitado pelo plano atual", restricted = true)
        }
        session.discoverTarget(phase, local.countExchanges())
    }

    private suspend fun syncExchangeMetadata(session: RunSession) {
        val phase = SyncPhase.EXCHANGE_METADATA
        session.startPhase(phase, "Atualizando corretoras")
        processMetadataPhase(
            session = session,
            phase = phase,
            selectPendingIds = { minimumFetchedAt, limit, offset ->
                local.exchangeIdsMissingMetadata(minimumFetchedAt, limit, offset)
            },
            download = remote::exchangeMetadata,
            persist = { page, items -> local.persistExchangeMetadata(session.runId, page, items) },
        )
    }

    private suspend fun syncCoins(session: RunSession) {
        val phase = SyncPhase.COINS
        if (session.quota.isInReserve(QUOTA_RESERVE_PERCENT)) {
            session.skip(phase, "Cota reservada para os dados essenciais", restricted = true)
            return
        }
        if (session.trigger == SyncTrigger.STARTUP_ESSENTIAL && local.countCoins() > 0) {
            session.skip(phase, "Catálogo completo é atualizado manualmente")
            return
        }

        session.startPhase(phase, "Baixando moedas")
        try {
            val total = requestWithRetry { remote.globalMetrics() }.totalCryptocurrencies
            if (total != null) session.discoverTarget(phase, total.toLong())
        } catch (_: SyncFailure) {
            // A métrica melhora o progresso, mas não é necessária para baixar o catálogo.
        }

        var firstPage = (session.committedPages.getValue(phase).maxOrNull() ?: 0) + 1
        var reachedLastPage = false
        var restrictedByPlan = false

        if (firstPage == 1) {
            session.requestPages(phase, 1)
            val items = try {
                requestWithRetry { remote.coinListings(start = 1, limit = NETWORK_PAGE_SIZE) }
            } catch (exception: SyncFailure) {
                if (exception.error is CryptoError.PlanUnavailable) {
                    local.markResourceFailure(COIN_RESOURCE_KEY, PLAN_UNAVAILABLE)
                    session.skip(phase, "Moedas indisponíveis no plano atual", restricted = true)
                    session.discoverTarget(phase, local.countCoins())
                    return
                }
                throw exception
            }
            local.persistCoinPage(session.runId, firstPage, items)
            session.commitPage(phase, items.size)
            reachedLastPage = items.size < NETWORK_PAGE_SIZE
            firstPage += 1
        }

        while (!reachedLastPage) {
            val pages = (firstPage until firstPage + config.parallelIoValue).toList()
            val pageResults = processPageWindow(
                session = session,
                phase = phase,
                pages = pages,
                download = { page ->
                    remote.coinListings(
                        start = ((page - 1) * NETWORK_PAGE_SIZE) + 1,
                        limit = NETWORK_PAGE_SIZE,
                    )
                },
                persist = { page, items -> local.persistCoinPage(session.runId, page, items) },
            )
            restrictedByPlan = restrictedByPlan || pageResults.any(PageCommit::restricted)

            reachedLastPage = restrictedByPlan || pageResults.any { !it.restricted && it.items < NETWORK_PAGE_SIZE }
            firstPage += config.parallelIoValue
        }

        if (!restrictedByPlan) local.markCoinsComplete(session.runId)
        local.markResourceSuccess(COIN_RESOURCE_KEY)
        if (restrictedByPlan) {
            session.skip(phase, "Catálogo de moedas limitado pelo plano atual", restricted = true)
        }
        session.discoverTarget(phase, local.countCoins())
    }

    private suspend fun syncCoinMetadata(session: RunSession) {
        val phase = SyncPhase.COIN_METADATA
        if (session.quota.isInReserve(QUOTA_RESERVE_PERCENT)) {
            session.skip(phase, "Metadados adiados para preservar a cota", restricted = true)
            return
        }
        if (session.trigger == SyncTrigger.STARTUP_ESSENTIAL && local.countCoins() > 0) {
            session.skip(phase, "Metadados completos são atualizados manualmente")
            return
        }

        session.startPhase(phase, "Atualizando moedas")
        processMetadataPhase(
            session = session,
            phase = phase,
            selectPendingIds = { minimumFetchedAt, limit, offset ->
                local.coinIdsMissingMetadata(minimumFetchedAt, limit, offset)
            },
            download = remote::coinMetadata,
            persist = { page, items -> local.persistCoinMetadata(session.runId, page, items) },
        )
    }

    private suspend fun <T> processMetadataPhase(
        session: RunSession,
        phase: SyncPhase,
        selectPendingIds: (minimumFetchedAt: Long, limit: Long, offset: Long) -> List<Long>,
        download: (ids: List<Long>) -> Flow<ApiResult<Map<String, T>>>,
        persist: suspend (page: Int, items: Map<String, T>) -> Unit,
    ) {
        val minimumFetchedAt = currentTimeMillis() - METADATA_CACHE_TTL_MILLIS
        val windowSize = METADATA_BATCH_SIZE.toLong() * config.parallelIoValue
        var nextPage = (session.committedPages.getValue(phase).maxOrNull() ?: 0) + 1
        var skippedMissingRows = 0L

        while (true) {
            val ids = selectPendingIds(
                minimumFetchedAt,
                windowSize,
                skippedMissingRows,
            )
            if (ids.isEmpty()) break

            val batches = ids.chunked(METADATA_BATCH_SIZE).map { batchIds ->
                MetadataBatchRequest(page = nextPage++, ids = batchIds)
            }
            session.requestPages(phase, batches.size)

            var missingRowsInWindow = 0L
            batches.asFlow()
                .processMetadataBatches(
                    parallelIo = config.parallelIoValue,
                    parallelDb = config.parallelDbValue,
                    bufferMultiplier = BUFFER_MULTIPLIER,
                    download = { batchIds -> requestWithRetry { download(batchIds) } },
                    persist = persist,
                )
                .collect { commit ->
                    missingRowsInWindow +=
                        (commit.requestedItems - commit.persistedItems).coerceAtLeast(0)
                    session.commitPage(phase, commit.persistedItems)
                }
            skippedMissingRows += missingRowsInWindow
        }
    }

    private suspend fun <T> processPageWindow(
        session: RunSession,
        phase: SyncPhase,
        pages: List<Int>,
        download: (Int) -> Flow<ApiResult<List<T>>>,
        persist: suspend (page: Int, items: List<T>) -> Unit,
    ): List<PageCommit> {
        val results = mutableListOf<PageCommit>()
        session.requestPages(phase, pages.size)

        pages.asFlow()
            .mapParallel(config.parallelIoValue) { page ->
                try {
                    PageDownload.Success(page, requestWithRetry { download(page) })
                } catch (exception: SyncFailure) {
                    if (exception.error is CryptoError.PlanUnavailable) {
                        PageDownload.Restricted
                    } else {
                        throw exception
                    }
                }
            }
            .buffer(config.parallelDbValue * BUFFER_MULTIPLIER)
            .mapParallel(config.parallelDbValue) { page ->
                when (page) {
                    is PageDownload.Success -> {
                        persist(page.page, page.items)
                        PageCommit(page.items.size, restricted = false)
                    }
                    PageDownload.Restricted -> PageCommit(0, restricted = true)
                }
            }
            .collect { result ->
                results += result
                if (!result.restricted) session.commitPage(phase, result.items)
            }

        return results
    }

    private suspend fun <T> requestWithRetry(request: () -> Flow<ApiResult<T>>): T {
        var attempt = 0
        while (true) {
            when (val result = request().first()) {
                is ApiResult.Success -> return result.data
                is ApiResult.Failure -> {
                    val transient = result.error is CryptoError.Timeout ||
                        result.error is CryptoError.NoConnection ||
                        result.error is CryptoError.ServerUnavailable ||
                        result.error is CryptoError.RateLimited
                    if (!transient || attempt >= MAX_RETRY_ATTEMPTS) {
                        throw SyncFailure(result.error)
                    }
                    val retryDelay = (result.error as? CryptoError.RateLimited)?.retryAfterMillis
                        ?: (INITIAL_RETRY_DELAY_MILLIS shl attempt.coerceAtMost(MAX_RETRY_SHIFT))
                    attempt += 1
                    delay(retryDelay)
                }
            }
        }
    }

    private suspend fun publishFinal(
        progress: SyncProgress,
        report: (SyncProgress) -> Unit,
    ) {
        if (progress.runId != null) local.updateRun(progress)
        report(progress)
    }

    private inner class RunSession(
        val runId: String,
        val trigger: SyncTrigger,
        val quota: ApiQuotaSnapshot,
        val committedPages: Map<SyncPhase, Set<Int>>,
        initialProgress: SyncProgress,
        private val report: (SyncProgress) -> Unit,
    ) {
        var progress = initialProgress
            private set
        var restricted = false
            private set
        private val targets = mutableMapOf<SyncPhase, Long>()

        suspend fun startPhase(phase: SyncPhase, message: String) {
            publish(progress.copy(phase = phase, message = message))
        }

        suspend fun requestPages(phase: SyncPhase, count: Int) {
            publish(
                progress.copy(
                    phase = phase,
                    requestedPages = progress.requestedPages + count,
                ),
            )
        }

        suspend fun commitPage(phase: SyncPhase, items: Int) {
            val persistedItems = if (phase == SyncPhase.COINS || phase == SyncPhase.EXCHANGES) items else 0
            publish(
                progress.copy(
                    phase = phase,
                    committedPages = progress.committedPages + 1,
                    persistedItems = progress.persistedItems + persistedItems,
                ),
            )
        }

        suspend fun skip(
            phase: SyncPhase,
            reason: String,
            restricted: Boolean = false,
        ) {
            this.restricted = this.restricted || restricted
            publish(progress.copy(phase = phase, message = reason))
        }

        suspend fun discoverTarget(phase: SyncPhase, items: Long) {
            targets[phase] = items
            publish(progress.copy(plannedItems = targets.values.sum()))
        }

        suspend fun complete() {
            publish(
                progress.copy(
                    phase = SyncPhase.COMPLETED,
                    status = if (restricted) SyncStatus.PARTIAL else SyncStatus.COMPLETED,
                    message = if (restricted) {
                        "Dados essenciais atualizados; etapas opcionais foram adiadas pela cota."
                    } else {
                        "Sincronização concluída."
                    },
                ),
            )
        }

        private suspend fun publish(next: SyncProgress) {
            local.updateRun(next)
            progress = next
            report(next)
        }
    }

    private sealed interface PageDownload<out T> {
        data class Success<T>(val page: Int, val items: List<T>) : PageDownload<T>
        data object Restricted : PageDownload<Nothing>
    }

    private data class PageCommit(
        val items: Int,
        val restricted: Boolean,
    )

    private companion object {
        const val NETWORK_PAGE_SIZE = 400
        const val METADATA_BATCH_SIZE = 250
        const val BUFFER_MULTIPLIER = 2
        const val MAX_RETRY_ATTEMPTS = 3
        const val MAX_RETRY_SHIFT = 4
        const val INITIAL_RETRY_DELAY_MILLIS = 1_000L
        const val QUOTA_RESERVE_PERCENT = 10
        const val EXCHANGE_CACHE_TTL_MILLIS = 60 * 60_000L
        const val METADATA_CACHE_TTL_MILLIS = 24 * 60 * 60_000L
        const val EXCHANGE_RESOURCE_KEY = "exchange_catalog"
        const val COIN_RESOURCE_KEY = "coin_catalog"
        const val PLAN_UNAVAILABLE = "PLAN_UNAVAILABLE"
    }
}

private class SyncFailure(val error: CryptoError) : RuntimeException()
