package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.repository.MarketRepository
import br.com.rmf.kmp.cryptoview.ui.model.ExchangeDetailUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExchangeDetailViewModel internal constructor(
    private val repository: MarketRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExchangeDetailUiState())
    val uiState: StateFlow<ExchangeDetailUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    fun load(exchangeId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            launch {
                repository.observeExchange(exchangeId).collect { exchange ->
                    _uiState.value = _uiState.value.copy(exchange = exchange)
                }
            }
            launch {
                repository.observeAssets(exchangeId).collect { assets ->
                    _uiState.value = _uiState.value.copy(assets = assets)
                }
            }
            launch {
                val error = repository.refreshExchangeAssets(exchangeId)
                _uiState.value = _uiState.value.copy(loadingAssets = false, error = error)
            }
        }
    }
}

