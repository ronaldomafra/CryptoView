package br.com.rmf.kmp.cryptoview.utils

import br.com.rmf.kmp.cryptoview.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal data class SyncPerformanceMetric(
    val calls: Long = 0,
    val items: Long = 0,
    val totalDurationMillis: Long = 0,
    val totalWaitMillis: Long = 0,
)

internal class SyncPerformanceTracker(
    private val nowMillis: () -> Long = ::currentTimeMillis,
    private val sink: (String) -> Unit = {},
) {
    private val metrics = MutableStateFlow<Map<String, SyncPerformanceMetric>>(emptyMap())

    fun mark(): Long = nowMillis()

    fun recordElapsed(
        operation: String,
        startedAt: Long,
        items: Int = 0,
        waitMillis: Long = 0,
    ) {
        record(
            operation = operation,
            durationMillis = (nowMillis() - startedAt).coerceAtLeast(0),
            items = items,
            waitMillis = waitMillis,
        )
    }

    fun record(
        operation: String,
        durationMillis: Long = 0,
        items: Int = 0,
        waitMillis: Long = 0,
    ) {
        metrics.update { current ->
            val previous = current[operation] ?: SyncPerformanceMetric()
            current + (operation to previous.copy(
                calls = previous.calls + 1,
                items = previous.items + items,
                totalDurationMillis = previous.totalDurationMillis + durationMillis.coerceAtLeast(0),
                totalWaitMillis = previous.totalWaitMillis + waitMillis.coerceAtLeast(0),
            ))
        }
    }

    fun snapshot(): Map<String, SyncPerformanceMetric> = metrics.value

    fun reset() {
        metrics.value = emptyMap()
    }

    fun flush() {
        metrics.value.entries.sortedBy { it.key }.forEach { (operation, metric) ->
            sink(
                "operation=$operation calls=${metric.calls} items=${metric.items} " +
                    "duration_ms=${metric.totalDurationMillis} wait_ms=${metric.totalWaitMillis}",
            )
        }
    }
}
