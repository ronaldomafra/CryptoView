package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.model.CoinSortOrder
import br.com.rmf.kmp.cryptoview.domain.model.CoinHistoryRange
import br.com.rmf.kmp.cryptoview.domain.model.CoinVariationFilter
import br.com.rmf.kmp.cryptoview.domain.model.SyncPhase
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.domain.repository.MarketRepository
import br.com.rmf.kmp.cryptoview.domain.sync.CryptoSyncManager
import br.com.rmf.kmp.cryptoview.ui.model.MarketUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MarketViewModel internal constructor(
    private val repository: MarketRepository,
    private val syncManager: CryptoSyncManager,
) : ViewModel() {
    private var activeQuery = ""
    private var activeSortOrder = CoinSortOrder.MARKET_CAP
    private var activeVariationFilter = CoinVariationFilter.ALL
    private var activeExchangeFilterId: Long? = null

    private var nextCoinOffset = 0
    private var nextExchangeOffset = 0
    private var coinGeneration = 0
    private var exchangeGeneration = 0
    private var coinLoadJob: Job? = null
    private var exchangeLoadJob: Job? = null
    private var queryJob: Job? = null
    private var detailJob: Job? = null
    private var historyJob: Job? = null
    private var pollingJob: Job? = null

    private val _uiState = MutableStateFlow(
        MarketUiState(pollingIntervalSeconds = POLLING_INTERVAL_MILLIS / 1_000L),
    )
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()
    val syncState = syncManager.state

    init {
        resetCoinPagination()
        resetExchangePagination()

        viewModelScope.launch {
            repository.observeCachedMarketExchanges(2).collect { exchanges ->
                _uiState.value = _uiState.value.copy(availableExchangeFilters = exchanges)
            }
        }
        viewModelScope.launch {
            var lastStatus: SyncStatus? = null
            var lastPhase: SyncPhase? = null
            syncManager.state.collect { progress ->
                if (progress.status == SyncStatus.COMPLETED && lastStatus != SyncStatus.COMPLETED) {
                    resetCoinPagination()
                    resetExchangePagination()
                } else if (progress.status == SyncStatus.RUNNING && progress.phase != lastPhase) {
                    when (progress.phase) {
                        SyncPhase.COINS -> if (_uiState.value.coins.isEmpty()) resetCoinPagination()
                        SyncPhase.EXCHANGES -> if (_uiState.value.exchanges.isEmpty()) resetExchangePagination()
                        else -> Unit
                    }
                }
                lastStatus = progress.status
                lastPhase = progress.phase
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

        val generation = coinGeneration
        val offset = nextCoinOffset
        _uiState.value = state.copy(coinsLoading = true)
        coinLoadJob = viewModelScope.launch {
            try {
                val page = repository.observeCoins(
                    query = activeQuery,
                    limit = PAGE_SIZE,
                    sortOrder = activeSortOrder,
                    variation = activeVariationFilter,
                    exchangeId = activeExchangeFilterId,
                    offset = offset,
                ).first()
                if (generation != coinGeneration) return@launch

                val current = if (offset == 0) emptyList() else _uiState.value.coins
                nextCoinOffset = offset + page.size
                _uiState.value = _uiState.value.copy(
                    coins = (current + page).distinctBy { it.id },
                    coinsLoading = false,
                    coinsHasMore = page.size == PAGE_SIZE,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                if (generation == coinGeneration) {
                    _uiState.value = _uiState.value.copy(coinsLoading = false, coinsHasMore = false)
                }
            }
        }
    }

    fun loadNextExchangesPage() {
        val state = _uiState.value
        if (state.exchangesLoading || !state.exchangesHasMore) return

        val generation = exchangeGeneration
        val offset = nextExchangeOffset
        _uiState.value = state.copy(exchangesLoading = true)
        exchangeLoadJob = viewModelScope.launch {
            try {
                val page = repository.observeExchanges(activeQuery, PAGE_SIZE, offset).first()
                if (generation != exchangeGeneration) return@launch

                val current = if (offset == 0) emptyList() else _uiState.value.exchanges
                nextExchangeOffset = offset + page.size
                _uiState.value = _uiState.value.copy(
                    exchanges = (current + page).distinctBy { it.id },
                    exchangesLoading = false,
                    exchangesHasMore = page.size == PAGE_SIZE,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                if (generation == exchangeGeneration) {
                    _uiState.value = _uiState.value.copy(exchangesLoading = false, exchangesHasMore = false)
                }
            }
        }
    }

    private fun resetCoinPagination() {
        coinGeneration += 1
        coinLoadJob?.cancel()
        nextCoinOffset = 0
        _uiState.value = _uiState.value.copy(
            coins = emptyList(),
            coinsLoading = false,
            coinsHasMore = true,
        )
        loadNextCoinsPage()
    }

    private fun resetExchangePagination() {
        exchangeGeneration += 1
        exchangeLoadJob?.cancel()
        nextExchangeOffset = 0
        _uiState.value = _uiState.value.copy(
            exchanges = emptyList(),
            exchangesLoading = false,
            exchangesHasMore = true,
        )
        loadNextExchangesPage()
    }

    fun expandCoin(id: Long?) {
        stopCoinDetails()
        if (id == null || _uiState.value.expandedCoinId == id) {
            _uiState.value = _uiState.value.copy(expandedCoinId = null)
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
                val result = repository.refreshCoinDetails(id)
                _uiState.value = _uiState.value.copy(
                    detailsLoading = false,
                    marketsError = result.marketsError,
                )
            }
        }
        loadHistory(id, CoinHistoryRange.HOURS_24)
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLLING_INTERVAL_MILLIS)
                if (_uiState.value.expandedCoinId != id) break
                repository.refreshQuote(id)
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
