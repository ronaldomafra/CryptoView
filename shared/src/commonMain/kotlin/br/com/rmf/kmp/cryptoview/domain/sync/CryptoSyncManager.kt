package br.com.rmf.kmp.cryptoview.domain.sync

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import br.com.rmf.kmp.cryptoview.data.api.CoinMarketCapRemoteDataSource
import br.com.rmf.kmp.cryptoview.data.database.MarketLocalDataSource
import br.com.rmf.kmp.cryptoview.domain.model.ApiQuotaSnapshot
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.SyncContext
import br.com.rmf.kmp.cryptoview.domain.model.SyncEvent
import br.com.rmf.kmp.cryptoview.domain.model.SyncExecutionResult
import br.com.rmf.kmp.cryptoview.domain.model.SyncPhase
import br.com.rmf.kmp.cryptoview.domain.model.SyncProgress
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.domain.model.SyncStepDecision
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStatus
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStorage
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

interface CryptoSyncManager {
    val state: StateFlow<SyncProgress>
    fun start(trigger: SyncTrigger): SyncExecutionResult
    fun resume(): SyncExecutionResult
    fun cancel()
}

internal class DefaultCryptoSyncManager(
    private val secureApiKeyStorage: SecureApiKeyStorage,
    private val remote: CoinMarketCapRemoteDataSource,
    private val local: MarketLocalDataSource,
    private val config: CryptoProcessConfig,
    private val steps: List<SyncStep>,
) : CryptoSyncManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val executionMutex = Mutex()
    private val _state = MutableStateFlow(SyncProgress())
    override val state: StateFlow<SyncProgress> = _state.asStateFlow()
    private var activeJob: Job? = null

    override fun start(trigger: SyncTrigger): SyncExecutionResult {
        if (activeJob?.isActive == true) return SyncExecutionResult.ALREADY_RUNNING
        activeJob = scope.launch { execute(trigger) }
        return SyncExecutionResult.STARTED
    }

    override fun resume(): SyncExecutionResult = start(SyncTrigger.RESUME)

    override fun cancel() {
        activeJob?.cancel(CancellationException("Sincronização interrompida pelo usuário"))
    }

    private suspend fun execute(requestedTrigger: SyncTrigger) {
        if (!executionMutex.tryLock()) return
        try {
            val keyStatus = secureApiKeyStorage.status()
            if (keyStatus != SecureApiKeyStatus.CONFIGURED) {
                _state.value = SyncProgress(
                    status = SyncStatus.FAILED,
                    phase = SyncPhase.VALIDATING_CREDENTIAL,
                    error = CryptoError.MissingApiKey,
                    message = "Configure uma API key antes de sincronizar.",
                )
                return
            }

            _state.value = SyncProgress(
                trigger = requestedTrigger,
                phase = SyncPhase.VALIDATING_CREDENTIAL,
                status = SyncStatus.RUNNING,
                message = "Validando acesso",
            )

            val keyInfo = when (val result = remote.keyInfo().first()) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> {
                    _state.value = _state.value.copy(
                        status = SyncStatus.FAILED,
                        error = result.error,
                        message = errorMessage(result.error),
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

            val resumable = if (requestedTrigger == SyncTrigger.MANUAL_FULL) {
                null
            } else {
                local.latestIncompleteRun()
            }
            val runId = resumable?.runId ?: "${currentTimeMillis()}-${requestedTrigger.name.lowercase()}"
            val trigger = resumable?.trigger ?: requestedTrigger
            val committedPages = SyncPhase.entries.associateWith { phase ->
                local.committedPages(runId, phase)
            }
            var progress = SyncProgress(
                runId = runId,
                trigger = trigger,
                phase = resumable?.phase ?: SyncPhase.PREPARING,
                status = SyncStatus.RUNNING,
                persistedItems = resumable?.persistedItems ?: 0,
                requestedPages = resumable?.requestedPages ?: 0,
                committedPages = resumable?.committedPages ?: 0,
                failedPages = resumable?.failedPages ?: 0,
                message = if (resumable == null) "Preparando sincronização" else "Retomando sincronização",
            )
            _state.value = progress
            if (resumable == null) local.createRun(progress) else local.updateRun(progress)

            val context = SyncContext(runId, trigger, quota, committedPages)
            val targets = mutableMapOf<SyncPhase, Long>()
            var restricted = false

            steps.forEach { step ->
                when (val decision = step.validate(context)) {
                    SyncStepDecision.Run -> Unit
                    is SyncStepDecision.Skip -> {
                        if (decision.reason.contains("cota", ignoreCase = true)) restricted = true
                        progress = progress.copy(phase = step.phase, message = decision.reason)
                        _state.value = progress
                        local.updateRun(progress)
                        return@forEach
                    }
                    is SyncStepDecision.Stop -> throw SyncStepException(decision.error)
                }

                step.execute(context).collect { event ->
                    progress = when (event) {
                        is SyncEvent.PhaseStarted -> progress.copy(
                            phase = event.phase,
                            message = event.message,
                        )
                        is SyncEvent.PageRequested -> progress.copy(
                            phase = event.phase,
                            requestedPages = progress.requestedPages + 1,
                        )
                        is SyncEvent.PageCommitted -> progress.copy(
                            phase = event.phase,
                            committedPages = progress.committedPages + 1,
                            persistedItems = progress.persistedItems + if (
                                event.phase == SyncPhase.COINS || event.phase == SyncPhase.EXCHANGES
                            ) event.items else 0,
                        )
                        is SyncEvent.StepSkipped -> progress.copy(
                            phase = event.phase,
                            message = event.reason,
                        ).also {
                            if (event.reason.contains("plano", ignoreCase = true) ||
                                event.reason.contains("cota", ignoreCase = true)
                            ) restricted = true
                        }
                        is SyncEvent.TargetDiscovered -> {
                            targets[event.phase] = event.items
                            progress.copy(plannedItems = targets.values.sum())
                        }
                    }
                    _state.value = progress
                    local.updateRun(progress)
                }
            }

            progress = progress.copy(
                phase = SyncPhase.COMPLETED,
                status = if (restricted) SyncStatus.PARTIAL else SyncStatus.COMPLETED,
                message = if (restricted) {
                    "Dados essenciais atualizados; etapas opcionais foram adiadas pela cota."
                } else {
                    "Sincronização concluída."
                },
            )
            _state.value = progress
            local.updateRun(progress)
        } catch (exception: CancellationException) {
            val paused = _state.value.copy(
                status = SyncStatus.PAUSED,
                message = "Sincronização pausada; o progresso foi preservado.",
            )
            _state.value = paused
            if (paused.runId != null) local.updateRun(paused)
        } catch (exception: SyncStepException) {
            val failed = _state.value.copy(
                status = SyncStatus.FAILED,
                failedPages = _state.value.failedPages + 1,
                error = exception.error,
                message = errorMessage(exception.error),
            )
            _state.value = failed
            if (failed.runId != null) local.updateRun(failed)
        } catch (exception: Throwable) {
            val error = CryptoError.Unknown(exception.message)
            val failed = _state.value.copy(
                status = SyncStatus.FAILED,
                error = error,
                message = errorMessage(error),
            )
            _state.value = failed
            if (failed.runId != null) local.updateRun(failed)
        } finally {
            executionMutex.unlock()
            activeJob = null
        }
    }
}

internal fun errorMessage(error: CryptoError): String = when (error) {
    CryptoError.NoConnection -> "Sem conexão. Os dados locais continuam disponíveis."
    CryptoError.Timeout -> "A conexão demorou mais que o esperado."
    CryptoError.MissingApiKey -> "API key não configurada."
    is CryptoError.InvalidApiKey -> "A API key foi rejeitada."
    is CryptoError.PlanUnavailable -> "Este recurso não está disponível no plano atual."
    is CryptoError.RateLimited -> "Limite temporário da API atingido."
    is CryptoError.ServerUnavailable -> "A CoinMarketCap está temporariamente indisponível."
    is CryptoError.InvalidResponse -> "A API retornou dados inválidos."
    is CryptoError.Serialization -> "Não foi possível interpretar os dados recebidos."
    is CryptoError.Unknown -> "Não foi possível concluir a operação."
}
