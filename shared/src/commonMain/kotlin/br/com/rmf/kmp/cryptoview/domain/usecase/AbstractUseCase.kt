package br.com.rmf.kmp.cryptoview.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

abstract class AbstractUseCase<T, Params> {
    protected abstract fun buildUseCaseFlow(params: Params): Flow<T>

    fun execute(
        params: Params,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): Flow<T> = buildUseCaseFlow(params).flowOn(backgroundDispatcher)
}
