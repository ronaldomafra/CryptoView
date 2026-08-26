package br.com.rmf.kmp.cryptoview.ui.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Keeps the loaded prefix of a paged database query subscribed to local changes.
 *
 * Increasing the visible limit restarts the query without discarding items already shown. Database
 * commits continue to update the current prefix, which lets synchronization batches appear in the
 * UI as soon as they are persisted.
 */
internal class ProgressivePagedObserver<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int,
    private val observe: (limit: Int) -> Flow<List<T>>,
    private val onLoading: () -> Unit,
    private val onItems: (items: List<T>, hasMore: Boolean) -> Unit,
    private val onFailure: () -> Unit,
) {
    private var observationJob: Job? = null
    private var generation = 0
    private var visibleLimit = pageSize
    private var loading = false
    private var hasMore = true

    fun reset() {
        visibleLimit = pageSize
        startObservation()
    }

    fun loadNext() {
        if (loading || !hasMore) return
        visibleLimit += pageSize
        startObservation()
    }

    fun cancel() {
        generation += 1
        observationJob?.cancel()
        observationJob = null
        loading = false
    }

    private fun startObservation() {
        val currentGeneration = ++generation
        observationJob?.cancel()
        loading = true
        onLoading()
        val requestedLimit = visibleLimit

        observationJob = scope.launch {
            try {
                observe(requestedLimit).collect { items ->
                    if (currentGeneration != generation) return@collect
                    loading = false
                    hasMore = items.size >= requestedLimit
                    onItems(items, hasMore)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                if (currentGeneration == generation) {
                    loading = false
                    hasMore = false
                    onFailure()
                }
            }
        }
    }
}
