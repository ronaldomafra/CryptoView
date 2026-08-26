package br.com.rmf.kmp.cryptoview.domain.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MetadataBatchPipelineTest {
    @Test
    fun metadataDownloadsRunInParallelAndDatabaseConcurrencyRemainsLimited() = runTest {
        var activeDownloads = 0
        var maximumDownloads = 0
        var startedDownloads = 0
        var activePersists = 0
        var maximumPersists = 0
        val firstParallelWindowStarted = CompletableDeferred<Unit>()
        val persistedPages = mutableSetOf<Int>()
        val requests = (1..6).map { page ->
            MetadataBatchRequest(page, listOf(page.toLong()))
        }

        val commits = requests.asFlow().processMetadataBatches(
            parallelIo = 3,
            parallelDb = 2,
            bufferMultiplier = 2,
            download = { ids ->
                activeDownloads += 1
                startedDownloads += 1
                maximumDownloads = maxOf(maximumDownloads, activeDownloads)
                if (startedDownloads == 3) firstParallelWindowStarted.complete(Unit)
                firstParallelWindowStarted.await()
                delay(10)
                activeDownloads -= 1
                ids.associate { id -> id.toString() to "metadata-$id" }
            },
            persist = { page, _ ->
                activePersists += 1
                maximumPersists = maxOf(maximumPersists, activePersists)
                delay(20)
                persistedPages += page
                activePersists -= 1
            },
        ).toList()

        assertEquals(3, maximumDownloads)
        assertTrue(maximumPersists in 1..2)
        assertEquals((1..6).toSet(), persistedPages)
        assertEquals(6, commits.sumOf(MetadataBatchCommit::persistedItems))
    }
}
