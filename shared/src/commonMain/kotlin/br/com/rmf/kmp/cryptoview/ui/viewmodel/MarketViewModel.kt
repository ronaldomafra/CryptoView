package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.domain.repository.MarketRepository
import br.com.rmf.kmp.cryptoview.domain.sync.CryptoSyncManager
import br.com.rmf.kmp.cryptoview.ui.model.MarketUiState
import br.com.rmf.kmp.cryptoview.utils.CryptoProcessConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class MarketViewModel internal constructor(
    private val repository: MarketRepository,
    private val syncManager: CryptoSyncManager,
    private val config: CryptoProcessConfig,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val limit = MutableStateFlow(50)
    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()
    val syncState = syncManager.state
    private var detailJob: Job? = null
    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            combine(query, limit) { text, count -> text to count }
                .flatMapLatest { (text, count) -> repository.observeCoins(text, count) }
                .collect { coins -> _uiState.value = _uiState.value.copy(coins = coins) }
        }
        viewModelScope.launch {
            combine(query, limit) { text, count -> text to count }
                .flatMapLatest { (text, count) -> repository.observeExchanges(text, count) }
                .collect { exchanges -> _uiState.value = _uiState.value.copy(exchanges = exchanges) }
        }
    }

    fun setQuery(value: String) {
        query.value = value
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun loadMore() {
        limit.value += 50
        _uiState.value = _uiState.value.copy(limit = limit.value)
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
            detailsLoading = true,
            marketsError = null,
            historyError = null,
        )
        detailJob = viewModelScope.launch {
            launch {
                repository.observeMarkets(id).collect { markets ->
                    _uiState.value = _uiState.value.copy(expandedMarkets = markets)
                }
            }
            launch {
                repository.observeHistory(id).collect { history ->
                    _uiState.value = _uiState.value.copy(expandedHistory = history)
                }
            }
            launch {
                val result = repository.refreshCoinDetails(id)
                _uiState.value = _uiState.value.copy(
                    detailsLoading = false,
                    marketsError = result.marketsError,
                    historyError = result.historyError,
                )
            }
        }
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(config.pollingIntervalMillis)
                if (_uiState.value.expandedCoinId != id) break
                repository.refreshQuote(id)
            }
        }
    }

    fun stopCoinDetails() {
        detailJob?.cancel()
        pollingJob?.cancel()
        detailJob = null
        pollingJob = null
    }

    fun synchronize() {
        syncManager.start(SyncTrigger.MANUAL_FULL)
    }

    fun cancelSync() = syncManager.cancel()
    fun resumeSync() = syncManager.resume()
}
