package br.com.rmf.kmp.cryptoview.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T, R> Flow<T>.mapParallel(
    concurrency: Int,
    transform: suspend (T) -> R,
): Flow<R> {
    require(concurrency > 0) { "concurrency deve ser maior que zero" }

    return flatMapMerge(concurrency = concurrency) { item ->
        flow { emit(transform(item)) }
    }
}
