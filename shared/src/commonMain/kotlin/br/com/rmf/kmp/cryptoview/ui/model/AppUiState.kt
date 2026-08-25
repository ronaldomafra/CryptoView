package br.com.rmf.kmp.cryptoview.ui.model

import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo

sealed interface AppUiState {
    data object Loading : AppUiState
    data class NeedsApiKey(
        val replacing: Boolean = false,
        val submitting: Boolean = false,
        val errorMessage: String? = null,
    ) : AppUiState
    data class Ready(
        val keyInfo: CoinMarketCapKeyInfo? = null,
        val validationMessage: String? = null,
    ) : AppUiState
    data class Unavailable(val message: String) : AppUiState
}

