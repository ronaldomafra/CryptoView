package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.domain.repository.MarketRepository
import br.com.rmf.kmp.cryptoview.domain.sync.CryptoSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsDataState(
    val coinCount: Long = 0,
    val exchangeCount: Long = 0,
    val clearing: Boolean = false,
)

class SettingsViewModel internal constructor(
    private val repository: MarketRepository,
    private val syncManager: CryptoSyncManager,
) : ViewModel() {
    private val _dataState = MutableStateFlow(SettingsDataState())
    val dataState: StateFlow<SettingsDataState> = _dataState.asStateFlow()
    val syncState = syncManager.state

    init {
        refreshCounts()
        viewModelScope.launch {
            syncState.collect { state ->
                if (state.status == SyncStatus.COMPLETED || state.status == SyncStatus.PARTIAL) refreshCounts()
            }
        }
    }

    fun synchronize() = syncManager.start(SyncTrigger.MANUAL_FULL)
    fun cancelSync() = syncManager.cancel()
    fun resumeSync() = syncManager.resume()

    fun clearCache() {
        viewModelScope.launch {
            _dataState.value = _dataState.value.copy(clearing = true)
            repository.clearCache()
            _dataState.value = SettingsDataState()
        }
    }

    private fun refreshCounts() {
        _dataState.value = _dataState.value.copy(
            coinCount = repository.coinCount(),
            exchangeCount = repository.exchangeCount(),
        )
    }
}
