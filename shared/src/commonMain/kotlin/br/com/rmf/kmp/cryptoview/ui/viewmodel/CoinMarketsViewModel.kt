package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.model.CoinExchangeMarket
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.repository.MarketRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CoinMarketsUiState(
    val coin: CoinSummary? = null,
    val markets: List<CoinExchangeMarket> = emptyList(),
    val loading: Boolean = true,
    val error: CryptoError? = null,
)

class CoinMarketsViewModel internal constructor(
    private val repository: MarketRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CoinMarketsUiState())
    val uiState: StateFlow<CoinMarketsUiState> = _uiState.asStateFlow()
    private var job: Job? = null

    fun load(coinId: Long) {
        job?.cancel()
        job = viewModelScope.launch {
            launch {
                repository.observeCoin(coinId).collect { coin ->
                    _uiState.value = _uiState.value.copy(coin = coin)
                }
            }
            launch {
                repository.observeMarkets(coinId).collect { markets ->
                    _uiState.value = _uiState.value.copy(markets = markets)
                }
            }
            launch {
                val error = repository.refreshMarkets(coinId)
                _uiState.value = _uiState.value.copy(loading = false, error = error)
            }
        }
    }
}
