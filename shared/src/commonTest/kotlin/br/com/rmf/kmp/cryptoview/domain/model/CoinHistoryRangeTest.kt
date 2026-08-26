package br.com.rmf.kmp.cryptoview.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoinHistoryRangeTest {
    @Test
    fun rangesMatchBasicPlanWindowsAndOneCreditLimit() {
        assertEquals("1h" to 24, CoinHistoryRange.HOURS_24.interval to CoinHistoryRange.HOURS_24.count)
        assertEquals("2h" to 84, CoinHistoryRange.DAYS_7.interval to CoinHistoryRange.DAYS_7.count)
        assertEquals("12h" to 60, CoinHistoryRange.DAYS_30.interval to CoinHistoryRange.DAYS_30.count)
        assertEquals("7d" to 52, CoinHistoryRange.YEAR_1.interval to CoinHistoryRange.YEAR_1.count)
        assertTrue(CoinHistoryRange.entries.all { it.count <= 100 })
    }
}
