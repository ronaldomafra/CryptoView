package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ApiRateLimiter {
    private val mutex = Mutex()
    private var nextRequestAt = 0L

    suspend fun acquire(requestsPerMinute: Int) {
        val safeLimit = requestsPerMinute.coerceAtLeast(1)
        val interval = 60_000L / safeLimit
        mutex.withLock {
            val now = currentTimeMillis()
            if (nextRequestAt > now) delay(nextRequestAt - now)
            nextRequestAt = currentTimeMillis() + interval
        }
    }
}

