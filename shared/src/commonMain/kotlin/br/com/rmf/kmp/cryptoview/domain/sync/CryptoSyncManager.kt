package br.com.rmf.kmp.cryptoview.domain.sync

import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.SyncExecutionResult
import br.com.rmf.kmp.cryptoview.domain.model.SyncProgress
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

interface CryptoSyncManager {
    val state: StateFlow<SyncProgress>
    fun start(trigger: SyncTrigger): SyncExecutionResult
    fun resume(): SyncExecutionResult
    fun cancel()
}

internal class DefaultCryptoSyncManager(
    private val coordinator: CryptoSyncCoordinator,
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
            coordinator.execute(requestedTrigger) { progress ->
                _state.value = progress
            }
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
    is CryptoError.NotFound -> "O recurso solicitado não foi encontrado."
    is CryptoError.InvalidResponse -> "A API retornou dados inválidos."
    is CryptoError.Serialization -> "Não foi possível interpretar os dados recebidos."
    is CryptoError.Unknown -> "Não foi possível concluir a operação."
}
