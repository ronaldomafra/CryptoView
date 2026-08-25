package br.com.rmf.kmp.cryptoview.ui.model

import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeAsset
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeSummary

data class ExchangeDetailUiState(
    val exchange: ExchangeSummary? = null,
    val assets: List<ExchangeAsset> = emptyList(),
    val loadingAssets: Boolean = true,
    val error: CryptoError? = null,
)

