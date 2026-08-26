package br.com.rmf.kmp.cryptoview.domain.sync

import br.com.rmf.kmp.cryptoview.domain.model.SyncPhase
import br.com.rmf.kmp.cryptoview.domain.model.SyncResumeData
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncExecutionPlanTest {
    @Test
    fun newExecutionsDoNotIncludeGlobalCoinMetadata() {
        val phases = syncExecutionPhases(resumable = null)

        assertEquals(
            listOf(SyncPhase.EXCHANGES, SyncPhase.EXCHANGE_METADATA, SyncPhase.COINS),
            phases,
        )
        assertFalse(SyncPhase.COIN_METADATA in phases)
    }

    @Test
    fun legacyRunPausedAtCoinMetadataCompletesWithoutAnotherRequest() {
        val resumable = SyncResumeData(
            runId = "legacy-run",
            trigger = SyncTrigger.FIRST_RUN,
            phase = SyncPhase.COIN_METADATA,
            persistedItems = 8_067,
            requestedPages = 24,
            committedPages = 24,
            failedPages = 0,
        )

        assertTrue(syncExecutionPhases(resumable).isEmpty())
    }
}
