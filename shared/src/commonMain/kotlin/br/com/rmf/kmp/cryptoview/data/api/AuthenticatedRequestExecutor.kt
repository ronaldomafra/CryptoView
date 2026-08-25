package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyResult
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class AuthenticatedRequestExecutor(
    private val secureApiKeyStorage: SecureApiKeyStorage,
) {
    fun <T> execute(
        request: (apiKey: String) -> Flow<ApiResult<T>>,
    ): Flow<ApiResult<T>> = flow {
        when (val stored = secureApiKeyStorage.read()) {
            is SecureApiKeyResult.Success -> emitAll(request(stored.value))
            is SecureApiKeyResult.Failure -> emit(ApiResult.Failure(CryptoError.MissingApiKey))
        }
    }
}

