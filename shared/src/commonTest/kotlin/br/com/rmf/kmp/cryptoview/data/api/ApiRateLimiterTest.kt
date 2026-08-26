package br.com.rmf.kmp.cryptoview.data.api

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiRateLimiterTest {
    @Test
    fun requestsWithinTheMinuteLimitAreReleasedAsABurst() = runTest {
        var now = 1_000L
        val waits = mutableListOf<Long>()
        val limiter = ApiRateLimiter(
            nowMillis = { now },
            wait = { millis ->
                waits += millis
                now += millis
            },
        )

        repeat(3) { assertEquals(0L, limiter.acquire(requestsPerMinute = 3)) }
        assertEquals(emptyList(), waits)

        assertEquals(60_000L, limiter.acquire(requestsPerMinute = 3))
        assertEquals(listOf(60_000L), waits)
    }
}
