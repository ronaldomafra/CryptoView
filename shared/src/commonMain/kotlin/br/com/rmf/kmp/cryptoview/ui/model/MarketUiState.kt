package br.com.rmf.kmp.cryptoview.ui.model

import br.com.rmf.kmp.cryptoview.domain.model.CoinExchangeMarket
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryPoint
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeSummary

data class MarketUiState(
    val coins: List<CoinSummary> = emptyList(),
    val exchanges: List<ExchangeSummary> = emptyList(),
    val query: String = "",
    val limit: Int = 50,
    val expandedCoinId: Long? = null,
    val expandedMarkets: List<CoinExchangeMarket> = emptyList(),
    val expandedHistory: List<CoinHistoryPoint> = emptyList(),
    val detailsLoading: Boolean = false,
    val marketsError: CryptoError? = null,
    val historyError: CryptoError? = null,
)

