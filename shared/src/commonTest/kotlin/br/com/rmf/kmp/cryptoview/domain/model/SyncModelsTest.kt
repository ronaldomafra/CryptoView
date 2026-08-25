package br.com.rmf.kmp.cryptoview.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncModelsTest {
    @Test
    fun quotaReserveUsesConfiguredPercentage() {
        assertTrue(ApiQuotaSnapshot(10_000, 9_200, 800, 30).isInReserve(10))
        assertFalse(ApiQuotaSnapshot(10_000, 8_000, 2_000, 30).isInReserve(10))
        assertFalse(ApiQuotaSnapshot(null, null, null, 30).isInReserve(10))
    }

    @Test
    fun progressPercentageIsBoundedAndUnknownWithoutTarget() {
        assertNull(SyncProgress(persistedItems = 10).percentage)
        assertEquals(0.5f, SyncProgress(plannedItems = 200, persistedItems = 100).percentage)
        assertEquals(1f, SyncProgress(plannedItems = 200, persistedItems = 300).percentage)
    }
}
