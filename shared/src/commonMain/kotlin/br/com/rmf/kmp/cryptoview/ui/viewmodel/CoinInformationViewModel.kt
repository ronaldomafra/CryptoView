package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.model.CoinInformation
import br.com.rmf.kmp.cryptoview.domain.model.CoinInformationFailure
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.repository.CoinInformationRepository
import br.com.rmf.kmp.cryptoview.domain.repository.MarketRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CoinInformationUiState(
    val coin: CoinSummary? = null,
    val information: CoinInformation? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val failure: CoinInformationFailure? = null,
)

class CoinInformationViewModel internal constructor(
    private val marketRepository: MarketRepository,
    private val informationRepository: CoinInformationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CoinInformationUiState())
    val uiState: StateFlow<CoinInformationUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var loadedCoinId: Long? = null
    private var loadedPaprikaId: String? = null

    fun load(coinId: Long, paprikaId: String) {
        loadJob?.cancel()
        loadedCoinId = coinId
        loadedPaprikaId = paprikaId
        _uiState.value = CoinInformationUiState()
        loadJob = viewModelScope.launch {
            launch {
                marketRepository.observeCoin(coinId).collect { coin ->
                    _uiState.value = _uiState.value.copy(coin = coin)
                }
            }
            launch {
                informationRepository.observeInformation(coinId).collect { information ->
                    _uiState.value = _uiState.value.copy(
                        information = information?.takeIf { it.paprikaId == paprikaId },
                    )
                }
            }
            launch { refresh(coinId, paprikaId, force = false) }
        }
    }

    fun retry() {
        val coinId = loadedCoinId ?: return
        val paprikaId = loadedPaprikaId ?: return
        viewModelScope.launch { refresh(coinId, paprikaId, force = true) }
    }

    private suspend fun refresh(coinId: Long, paprikaId: String, force: Boolean) {
        val coin = marketRepository.observeCoin(coinId).first()
        if (coin == null) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                refreshing = false,
                failure = CoinInformationFailure.UnresolvedIdentity,
            )
            return
        }

        val hasCache = _uiState.value.information != null
        _uiState.value = _uiState.value.copy(
            coin = coin,
            loading = !hasCache,
            refreshing = hasCache,
            failure = null,
        )
        val failure = informationRepository.refreshInformation(coin, paprikaId, force)
        _uiState.value = _uiState.value.copy(
            loading = false,
            refreshing = false,
            failure = failure,
        )
    }
}
