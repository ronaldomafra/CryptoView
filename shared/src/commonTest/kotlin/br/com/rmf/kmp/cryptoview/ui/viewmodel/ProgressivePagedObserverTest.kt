package br.com.rmf.kmp.cryptoview.ui.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressivePagedObserverTest {
    @Test
    fun committedBatchesUpdateTheVisibleListWithoutRestartingPagination() = runTest {
        val databaseRows = MutableStateFlow(emptyList<Int>())
        var visibleItems = emptyList<Int>()
        var hasMore = true

        val observer = ProgressivePagedObserver(
            scope = this,
            pageSize = 30,
            observe = { limit -> databaseRows.map { rows -> rows.take(limit) } },
            onLoading = {},
            onItems = { items, more ->
                visibleItems = items
                hasMore = more
            },
            onFailure = {},
        )

        observer.reset()
        runCurrent()
        assertEquals(emptyList(), visibleItems)
        assertFalse(hasMore)

        databaseRows.value = (1..45).toList()
        runCurrent()
        assertEquals((1..30).toList(), visibleItems)
        assertTrue(hasMore)

        observer.loadNext()
        runCurrent()
        assertEquals((1..45).toList(), visibleItems)
        assertFalse(hasMore)

        databaseRows.value = (1..75).toList()
        runCurrent()
        assertEquals((1..60).toList(), visibleItems)
        assertTrue(hasMore)

        observer.cancel()
    }
}
