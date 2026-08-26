package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ApiRateLimiter(
    private val nowMillis: () -> Long = ::currentTimeMillis,
    private val wait: suspend (Long) -> Unit = { delay(it) },
) {
    private val mutex = Mutex()
    private var windowStartedAt: Long? = null
    private var requestsInWindow = 0

    suspend fun acquire(requestsPerMinute: Int) {
        val safeLimit = requestsPerMinute.coerceAtLeast(1)

        while (true) {
            var waitMillis = 0L
            val granted = mutex.withLock {
                val now = nowMillis()
                val startedAt = windowStartedAt
                if (startedAt == null || now < startedAt || now - startedAt >= WINDOW_MILLIS) {
                    windowStartedAt = now
                    requestsInWindow = 0
                }

                if (requestsInWindow < safeLimit) {
                    requestsInWindow += 1
                    true
                } else {
                    waitMillis = WINDOW_MILLIS - (now - requireNotNull(windowStartedAt))
                    false
                }
            }

            if (granted) return
            wait(waitMillis.coerceAtLeast(1L))
        }
    }

    private companion object {
        const val WINDOW_MILLIS = 60_000L
    }
}
