package br.com.rmf.kmp.cryptoview.ui.model

import br.com.rmf.kmp.cryptoview.domain.model.CoinExchangeMarket
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryPoint
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryRange
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.CoinSortOrder
import br.com.rmf.kmp.cryptoview.domain.model.CoinVariationFilter
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeSummary

data class MarketUiState(
    val coins: List<CoinSummary> = emptyList(),
    val exchanges: List<ExchangeSummary> = emptyList(),
    val availableExchangeFilters: List<ExchangeSummary> = emptyList(),
    val query: String = "",
    val searchVisible: Boolean = false,
    val filtersVisible: Boolean = false,
    val sortOrder: CoinSortOrder = CoinSortOrder.MARKET_CAP,
    val variationFilter: CoinVariationFilter = CoinVariationFilter.ALL,
    val selectedExchangeId: Long? = null,
    val appliedSortOrder: CoinSortOrder = CoinSortOrder.MARKET_CAP,
    val appliedVariationFilter: CoinVariationFilter = CoinVariationFilter.ALL,
    val appliedExchangeId: Long? = null,
    val coinsLoading: Boolean = false,
    val coinsHasMore: Boolean = true,
    val exchangesLoading: Boolean = false,
    val exchangesHasMore: Boolean = true,
    val expandedCoinId: Long? = null,
    val expandedMarkets: List<CoinExchangeMarket> = emptyList(),
    val expandedHistory: List<CoinHistoryPoint> = emptyList(),
    val selectedHistoryRange: CoinHistoryRange = CoinHistoryRange.HOURS_24,
    val historyLoading: Boolean = false,
    val detailsLoading: Boolean = false,
    val marketsError: CryptoError? = null,
    val historyError: CryptoError? = null,
    val pollingIntervalSeconds: Long = 30,
)
