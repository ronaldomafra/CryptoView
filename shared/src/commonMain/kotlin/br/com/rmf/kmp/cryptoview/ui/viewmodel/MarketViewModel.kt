package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.model.CoinSortOrder
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryRange
import br.com.rmf.kmp.cryptoview.domain.model.CoinVariationFilter
import br.com.rmf.kmp.cryptoview.domain.model.CoinInformationFailure
import br.com.rmf.kmp.cryptoview.domain.model.CoinPaprikaIdResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.domain.repository.MarketRepository
import br.com.rmf.kmp.cryptoview.domain.repository.CoinInformationRepository
import br.com.rmf.kmp.cryptoview.domain.sync.CryptoSyncManager
import br.com.rmf.kmp.cryptoview.ui.model.MarketUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MarketViewModel internal constructor(
    private val repository: MarketRepository,
    private val informationRepository: CoinInformationRepository,
    private val syncManager: CryptoSyncManager,
) : ViewModel() {
    private var activeQuery = ""
    private var activeSortOrder = CoinSortOrder.MARKET_CAP
    private var activeVariationFilter = CoinVariationFilter.ALL
    private var activeExchangeFilterId: Long? = null

    private var queryJob: Job? = null
    private var detailJob: Job? = null
    private var historyJob: Job? = null
    private var pollingJob: Job? = null

    private val _uiState = MutableStateFlow(
        MarketUiState(pollingIntervalSeconds = POLLING_INTERVAL_MILLIS / 1_000L),
    )
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()
    val syncState = syncManager.state

    private val coinObserver = ProgressivePagedObserver(
        scope = viewModelScope,
        pageSize = PAGE_SIZE,
        observe = { limit ->
            repository.observeCoins(
                query = activeQuery,
                limit = limit,
                sortOrder = activeSortOrder,
                variation = activeVariationFilter,
                exchangeId = activeExchangeFilterId,
            )
        },
        onLoading = {
            _uiState.value = _uiState.value.copy(coinsLoading = true)
        },
        onItems = { coins, hasMore ->
            _uiState.value = _uiState.value.copy(
                coins = coins,
                coinsLoading = false,
                coinsHasMore = hasMore,
            )
        },
        onFailure = {
            _uiState.value = _uiState.value.copy(coinsLoading = false, coinsHasMore = false)
        },
    )

    private val exchangeObserver = ProgressivePagedObserver(
        scope = viewModelScope,
        pageSize = PAGE_SIZE,
        observe = { limit -> repository.observeExchanges(activeQuery, limit) },
        onLoading = {
            _uiState.value = _uiState.value.copy(exchangesLoading = true)
        },
        onItems = { exchanges, hasMore ->
            _uiState.value = _uiState.value.copy(
                exchanges = exchanges,
                exchangesLoading = false,
                exchangesHasMore = hasMore,
            )
        },
        onFailure = {
            _uiState.value = _uiState.value.copy(exchangesLoading = false, exchangesHasMore = false)
        },
    )

    init {
        resetCoinPagination()
        resetExchangePagination()

        viewModelScope.launch {
            repository.observeCachedMarketExchanges(2).collect { exchanges ->
                _uiState.value = _uiState.value.copy(availableExchangeFilters = exchanges)
            }
        }
    }

    fun setQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            delay(QUERY_DEBOUNCE_MILLIS)
            activeQuery = value
            resetCoinPagination()
            resetExchangePagination()
        }
    }

    fun showSearch() {
        _uiState.value = _uiState.value.copy(searchVisible = true, filtersVisible = false)
    }

    fun hideSearch() {
        queryJob?.cancel()
        activeQuery = ""
        _uiState.value = _uiState.value.copy(query = "", searchVisible = false)
        resetCoinPagination()
        resetExchangePagination()
    }

    fun toggleFilters() {
        val opening = !_uiState.value.filtersVisible
        _uiState.value = _uiState.value.copy(
            filtersVisible = opening,
            sortOrder = if (opening) activeSortOrder else _uiState.value.sortOrder,
            variationFilter = if (opening) activeVariationFilter else _uiState.value.variationFilter,
            selectedExchangeId = if (opening) activeExchangeFilterId else _uiState.value.selectedExchangeId,
        )
    }

    fun setSortOrder(value: CoinSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = value)
    }

    fun setVariationFilter(value: CoinVariationFilter) {
        _uiState.value = _uiState.value.copy(variationFilter = value)
    }

    fun setExchangeFilter(id: Long?) {
        _uiState.value = _uiState.value.copy(selectedExchangeId = id)
    }

    fun clearFilterDraft() {
        _uiState.value = _uiState.value.copy(
            sortOrder = CoinSortOrder.MARKET_CAP,
            variationFilter = CoinVariationFilter.ALL,
            selectedExchangeId = null,
        )
    }

    fun applyFilters() {
        val state = _uiState.value
        activeSortOrder = state.sortOrder
        activeVariationFilter = state.variationFilter
        activeExchangeFilterId = state.selectedExchangeId
        _uiState.value = state.copy(
            filtersVisible = false,
            appliedSortOrder = state.sortOrder,
            appliedVariationFilter = state.variationFilter,
            appliedExchangeId = state.selectedExchangeId,
        )
        resetCoinPagination()
    }

    fun loadNextCoinsPage() {
        val state = _uiState.value
        if (state.coinsLoading || !state.coinsHasMore) return
        coinObserver.loadNext()
    }

    fun loadNextExchangesPage() {
        val state = _uiState.value
        if (state.exchangesLoading || !state.exchangesHasMore) return
        exchangeObserver.loadNext()
    }

    private fun resetCoinPagination() {
        _uiState.value = _uiState.value.copy(
            coins = emptyList(),
            coinsLoading = true,
            coinsHasMore = true,
        )
        coinObserver.reset()
    }

    private fun resetExchangePagination() {
        _uiState.value = _uiState.value.copy(
            exchanges = emptyList(),
            exchangesLoading = true,
            exchangesHasMore = true,
        )
        exchangeObserver.reset()
    }

    fun expandCoin(id: Long?) {
        val selectedCoin = id?.let { coinId -> _uiState.value.coins.firstOrNull { it.id == coinId } }
        stopCoinDetails()
        if (id == null || _uiState.value.expandedCoinId == id) {
            _uiState.value = _uiState.value.copy(
                expandedCoinId = null,
                resolvedPaprikaId = null,
                paprikaIdLoading = false,
                paprikaIdFailure = null,
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            expandedCoinId = id,
            expandedMarkets = emptyList(),
            expandedHistory = emptyList(),
            selectedHistoryRange = CoinHistoryRange.HOURS_24,
            historyLoading = true,
            detailsLoading = true,
            marketsError = null,
            historyError = null,
            resolvedPaprikaId = null,
            paprikaIdLoading = selectedCoin != null,
            paprikaIdFailure = if (selectedCoin == null) {
                CoinInformationFailure.UnresolvedIdentity
            } else {
                null
            },
        )
        detailJob = viewModelScope.launch {
            launch {
                repository.observeCoin(id).collect { updatedCoin ->
                    if (updatedCoin != null) {
                        _uiState.value = _uiState.value.copy(
                            coins = _uiState.value.coins.map { coin ->
                                if (coin.id == updatedCoin.id) updatedCoin else coin
                            },
                        )
                    }
                }
            }
            launch {
                repository.observeMarkets(id).collect { markets ->
                    _uiState.value = _uiState.value.copy(expandedMarkets = markets)
                }
            }
            launch {
                informationRepository.observeMapping(id).collect { mapping ->
                    if (_uiState.value.expandedCoinId == id) {
                        _uiState.value = _uiState.value.copy(
                            resolvedPaprikaId = mapping?.paprikaId,
                        )
                    }
                }
            }
            launch {
                val result = repository.refreshCoinDetails(id)
                _uiState.value = _uiState.value.copy(
                    detailsLoading = false,
                    marketsError = result.marketsError,
                )
            }
        }
        loadHistory(id, CoinHistoryRange.HOURS_24)
        selectedCoin?.let(::resolveCoinPaprikaId)
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLLING_INTERVAL_MILLIS)
                if (_uiState.value.expandedCoinId != id) break
                repository.refreshQuote(id)
            }
        }
    }

    fun retryCoinPaprikaResolution() {
        val coinId = _uiState.value.expandedCoinId ?: return
        val coin = _uiState.value.coins.firstOrNull { it.id == coinId } ?: return
        resolveCoinPaprikaId(coin, force = true)
    }

    private fun resolveCoinPaprikaId(coin: CoinSummary, force: Boolean = false) {
        val coinId = coin.id
        if (_uiState.value.expandedCoinId == coinId) {
            _uiState.value = _uiState.value.copy(
                paprikaIdLoading = true,
                paprikaIdFailure = null,
            )
        }
        viewModelScope.launch {
            val result = informationRepository.resolveCoinId(coin, force)
            if (_uiState.value.expandedCoinId != coinId) return@launch
            _uiState.value = when (result) {
                is CoinPaprikaIdResult.Resolved -> _uiState.value.copy(
                    resolvedPaprikaId = result.paprikaId,
                    paprikaIdLoading = false,
                    paprikaIdFailure = null,
                )
                is CoinPaprikaIdResult.Failure -> _uiState.value.copy(
                    resolvedPaprikaId = null,
                    paprikaIdLoading = false,
                    paprikaIdFailure = result.reason,
                )
            }
        }
    }

    fun selectHistoryRange(range: CoinHistoryRange) {
        val coinId = _uiState.value.expandedCoinId ?: return
        if (_uiState.value.selectedHistoryRange == range) return
        historyJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedHistoryRange = range,
            expandedHistory = emptyList(),
            historyLoading = true,
            historyError = null,
        )
        loadHistory(coinId, range)
    }

    private fun loadHistory(coinId: Long, range: CoinHistoryRange) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            launch {
                repository.observeHistory(coinId, range).collect { history ->
                    val state = _uiState.value
                    if (state.expandedCoinId == coinId && state.selectedHistoryRange == range) {
                        _uiState.value = state.copy(expandedHistory = history)
                    }
                }
            }
            val error = repository.refreshHistory(coinId, range, force = true)
            val state = _uiState.value
            if (state.expandedCoinId == coinId && state.selectedHistoryRange == range) {
                _uiState.value = state.copy(historyLoading = false, historyError = error)
            }
        }
    }

    fun stopCoinDetails() {
        detailJob?.cancel()
        historyJob?.cancel()
        pollingJob?.cancel()
        detailJob = null
        historyJob = null
        pollingJob = null
    }

    fun synchronize() {
        syncManager.start(SyncTrigger.MANUAL_FULL)
    }

    fun cancelSync() = syncManager.cancel()
    fun resumeSync() = syncManager.resume()

    private companion object {
        const val PAGE_SIZE = 30
        const val QUERY_DEBOUNCE_MILLIS = 250L
        const val POLLING_INTERVAL_MILLIS = 60_000L
    }
}
