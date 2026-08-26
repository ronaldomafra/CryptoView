package br.com.rmf.kmp.cryptoview.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SyncExtensionsTest {
    @Test
    fun mapParallelRespectsConcurrencyAndProcessesEveryItem() = runTest {
        var active = 0
        var maximumActive = 0

        val result = (1..12)
            .asFlow()
            .mapParallel(concurrency = 3) { item ->
                active += 1
                maximumActive = maxOf(maximumActive, active)
                delay(10)
                active -= 1
                item * 2
            }
            .toList()

        assertEquals(3, maximumActive)
        assertEquals((1..12).map { it * 2 }, result.sorted())
    }

    @Test
    fun mapParallelRejectsNonPositiveConcurrency() {
        assertFailsWith<IllegalArgumentException> {
            listOf(1).asFlow().mapParallel(concurrency = 0) { it }
        }
    }

    @Test
    fun mapParallelPropagatesFailureAndCancelsSiblingWork() = runTest {
        var completedSiblingWork = 0

        assertFailsWith<IllegalStateException> {
            listOf(1, 2, 3)
                .asFlow()
                .mapParallel(concurrency = 3) { item ->
                    if (item == 2) error("falha esperada")
                    delay(1_000)
                    completedSiblingWork += 1
                }
                .toList()
        }

        assertEquals(0, completedSiblingWork)
    }

    @Test
    fun processConfigAcceptsOnlyPositiveParallelism() {
        assertFailsWith<IllegalArgumentException> {
            CryptoProcessConfig(parallelIoValue = 0, parallelDbValue = 1)
        }
        assertFailsWith<IllegalArgumentException> {
            CryptoProcessConfig(parallelIoValue = 1, parallelDbValue = 0)
        }
    }
}
