package br.com.rmf.kmp.cryptoview.domain.sync

import br.com.rmf.kmp.cryptoview.domain.model.SyncExecutionResult
import br.com.rmf.kmp.cryptoview.domain.model.SyncProgress
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoSyncManagerTest {
    @Test
    fun managerDelegatesExecutionAndPublishesCoordinatorProgress() = runBlocking {
        val coordinator = CryptoSyncCoordinator { trigger, report ->
            report(SyncProgress(trigger = trigger, status = SyncStatus.RUNNING))
            report(SyncProgress(trigger = trigger, status = SyncStatus.COMPLETED))
        }
        val manager = DefaultCryptoSyncManager(coordinator)

        assertEquals(SyncExecutionResult.STARTED, manager.start(SyncTrigger.MANUAL_FULL))

        val completed = withTimeout(5_000) {
            manager.state.first { it.status == SyncStatus.COMPLETED }
        }
        assertEquals(SyncTrigger.MANUAL_FULL, completed.trigger)
        Unit
    }

    @Test
    fun managerRejectsSecondExecutionWhileCoordinatorIsRunning() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val coordinator = CryptoSyncCoordinator { trigger, report ->
            report(SyncProgress(trigger = trigger, status = SyncStatus.RUNNING))
            started.complete(Unit)
            release.await()
            report(SyncProgress(trigger = trigger, status = SyncStatus.COMPLETED))
        }
        val manager = DefaultCryptoSyncManager(coordinator)

        assertEquals(SyncExecutionResult.STARTED, manager.start(SyncTrigger.FIRST_RUN))
        withTimeout(5_000) { started.await() }
        assertEquals(SyncExecutionResult.ALREADY_RUNNING, manager.start(SyncTrigger.MANUAL_FULL))

        release.complete(Unit)
        withTimeout(5_000) {
            manager.state.first { it.status == SyncStatus.COMPLETED }
        }
        Unit
    }

    @Test
    fun cancelStopsTheActiveCoordinatorExecution() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val coordinator = CryptoSyncCoordinator { _, _ ->
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val manager = DefaultCryptoSyncManager(coordinator)

        assertEquals(SyncExecutionResult.STARTED, manager.start(SyncTrigger.MANUAL_FULL))
        withTimeout(5_000) { started.await() }
        manager.cancel()
        withTimeout(5_000) { cancelled.await() }
        Unit
    }
}
