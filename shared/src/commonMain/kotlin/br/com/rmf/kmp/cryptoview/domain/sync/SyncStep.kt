package br.com.rmf.kmp.cryptoview.domain.sync

import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.SyncContext
import br.com.rmf.kmp.cryptoview.domain.model.SyncEvent
import br.com.rmf.kmp.cryptoview.domain.model.SyncPhase
import br.com.rmf.kmp.cryptoview.domain.model.SyncStepDecision
import kotlinx.coroutines.flow.Flow

internal interface SyncStep {
    val phase: SyncPhase
    suspend fun validate(context: SyncContext): SyncStepDecision = SyncStepDecision.Run
    fun execute(context: SyncContext): Flow<SyncEvent>
}

internal class SyncStepException(val error: CryptoError) : RuntimeException()

