package br.com.rmf.kmp.cryptoview.ui.model

import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo

sealed interface CoinMarketCapTestUiState {
    data object Idle : CoinMarketCapTestUiState
    data class Loading(val testName: String) : CoinMarketCapTestUiState
    data class Completed(
        val testName: String,
        val result: ApiResult<CoinMarketCapKeyInfo>,
    ) : CoinMarketCapTestUiState

    data class UnexpectedFailure(
        val testName: String,
        val detail: String?,
    ) : CoinMarketCapTestUiState
}
