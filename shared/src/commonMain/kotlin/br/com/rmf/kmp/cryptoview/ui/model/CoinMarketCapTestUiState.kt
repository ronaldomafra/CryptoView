package br.com.rmf.kmp.cryptoview.ui.model

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo

sealed interface CoinMarketCapTestUiState {
    data object Idle : CoinMarketCapTestUiState
    data class Loading(val testName: String) : CoinMarketCapTestUiState
    data class Completed(
        val testName: String,
        val result: ApiResult<CoinMarketCapKeyInfo>,
        val storageMessage: String? = null,
    ) : CoinMarketCapTestUiState

    data class StorageCompleted(
        val testName: String,
        val message: String,
        val successful: Boolean,
    ) : CoinMarketCapTestUiState

    data class UnexpectedFailure(
        val testName: String,
        val detail: String?,
    ) : CoinMarketCapTestUiState
}
