package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.repository.CoinMarketCapDataRepository
import br.com.rmf.kmp.cryptoview.ui.model.CoinMarketCapTestUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CoinMarketCapTestViewModel internal constructor(
    private val repository: CoinMarketCapDataRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CoinMarketCapTestUiState>(
        CoinMarketCapTestUiState.Idle,
    )
    val uiState: StateFlow<CoinMarketCapTestUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null

    fun executeKeyInfoTest(testName: String, apiKey: String) {
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            _uiState.value = CoinMarketCapTestUiState.Loading(testName)
            try {
                repository.getKeyInfo(apiKey).collect { result ->
                    _uiState.value = CoinMarketCapTestUiState.Completed(testName, result)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _uiState.value = CoinMarketCapTestUiState.UnexpectedFailure(
                    testName = testName,
                    detail = exception.message,
                )
            }
        }
    }

    fun clearResult() {
        requestJob?.cancel()
        requestJob = null
        _uiState.value = CoinMarketCapTestUiState.Idle
    }
}
