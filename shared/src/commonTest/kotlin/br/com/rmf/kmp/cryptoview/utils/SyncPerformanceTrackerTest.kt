package br.com.rmf.kmp.cryptoview.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncPerformanceTrackerTest {
    @Test
    fun aggregatesCallsItemsDurationsAndRateLimitWait() {
        var now = 100L
        val lines = mutableListOf<String>()
        val tracker = SyncPerformanceTracker(nowMillis = { now }, sink = lines::add)

        val startedAt = tracker.mark()
        now = 135L
        tracker.recordElapsed("database.coins", startedAt, items = 400)
        tracker.record("database.coins", durationMillis = 25, items = 200)
        tracker.record("rate_limit.coin_listings", waitMillis = 60_000)

        assertEquals(
            SyncPerformanceMetric(calls = 2, items = 600, totalDurationMillis = 60),
            tracker.snapshot().getValue("database.coins"),
        )
        assertEquals(60_000, tracker.snapshot().getValue("rate_limit.coin_listings").totalWaitMillis)

        tracker.flush()
        assertEquals(2, lines.size)
    }
}
