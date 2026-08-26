package br.com.rmf.kmp.cryptoview.domain.sync

import br.com.rmf.kmp.cryptoview.utils.mapParallel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer

internal data class MetadataBatchRequest(
    val page: Int,
    val ids: List<Long>,
)

internal data class MetadataBatchCommit(
    val page: Int,
    val requestedItems: Int,
    val persistedItems: Int,
)

internal fun <T> Flow<MetadataBatchRequest>.processMetadataBatches(
    parallelIo: Int,
    parallelDb: Int,
    bufferMultiplier: Int,
    download: suspend (ids: List<Long>) -> Map<String, T>,
    persist: suspend (page: Int, items: Map<String, T>) -> Unit,
): Flow<MetadataBatchCommit> {
    require(bufferMultiplier > 0) { "bufferMultiplier deve ser maior que zero" }

    return mapParallel(parallelIo) { request ->
        DownloadedMetadataBatch(
            request = request,
            items = download(request.ids),
        )
    }.buffer(parallelDb * bufferMultiplier)
        .mapParallel(parallelDb) { batch ->
            persist(batch.request.page, batch.items)
            MetadataBatchCommit(
                page = batch.request.page,
                requestedItems = batch.request.ids.size,
                persistedItems = batch.items.size,
            )
        }
}

private data class DownloadedMetadataBatch<T>(
    val request: MetadataBatchRequest,
    val items: Map<String, T>,
)
