package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.domain.model.SyncTrigger
import br.com.rmf.kmp.cryptoview.domain.repository.CoinMarketCapDataRepository
import br.com.rmf.kmp.cryptoview.domain.repository.MarketRepository
import br.com.rmf.kmp.cryptoview.domain.sync.CryptoSyncManager
import br.com.rmf.kmp.cryptoview.domain.sync.errorMessage
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyResult
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStatus
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStorage
import br.com.rmf.kmp.cryptoview.ui.model.AppUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppViewModel internal constructor(
    private val secureApiKeyStorage: SecureApiKeyStorage,
    private val keyRepository: CoinMarketCapDataRepository,
    private val marketRepository: MarketRepository,
    private val syncManager: CryptoSyncManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AppUiState>(AppUiState.Loading)
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        inspectCredential()
    }

    fun validateAndSave(apiKey: String) {
        val current = _uiState.value as? AppUiState.NeedsApiKey
        _uiState.value = AppUiState.NeedsApiKey(
            replacing = current?.replacing == true,
            submitting = true,
        )
        viewModelScope.launch {
            try {
                when (val result = keyRepository.getKeyInfo(apiKey).first()) {
                    is ApiResult.Failure -> _uiState.value = AppUiState.NeedsApiKey(
                        replacing = current?.replacing == true,
                        errorMessage = errorMessage(result.error),
                    )
                    is ApiResult.Success -> when (secureApiKeyStorage.save(apiKey)) {
                        is SecureApiKeyResult.Success -> {
                            _uiState.value = AppUiState.Ready(
                                keyInfo = result.data,
                                validationMessage = "API key validada com segurança.",
                            )
                            syncManager.start(
                                if (marketRepository.coinCount() == 0L) {
                                    SyncTrigger.FIRST_RUN
                                } else {
                                    SyncTrigger.STARTUP_ESSENTIAL
                                },
                            )
                        }
                        is SecureApiKeyResult.Failure -> _uiState.value = AppUiState.NeedsApiKey(
                            replacing = current?.replacing == true,
                            errorMessage = "Não foi possível proteger a API key neste dispositivo.",
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                _uiState.value = AppUiState.NeedsApiKey(
                    replacing = current?.replacing == true,
                    errorMessage = "Não foi possível concluir a validação.",
                )
            }
        }
    }

    fun revalidate() {
        viewModelScope.launch {
            when (val stored = secureApiKeyStorage.read()) {
                is SecureApiKeyResult.Failure -> inspectCredential()
                is SecureApiKeyResult.Success -> when (val result = keyRepository.getKeyInfo(stored.value).first()) {
                    is ApiResult.Success -> _uiState.value = AppUiState.Ready(
                        keyInfo = result.data,
                        validationMessage = "Credencial validada agora.",
                    )
                    is ApiResult.Failure -> {
                        val ready = _uiState.value as? AppUiState.Ready
                        _uiState.value = AppUiState.Ready(
                            keyInfo = ready?.keyInfo,
                            validationMessage = errorMessage(result.error),
                        )
                    }
                }
            }
        }
    }

    fun beginReplacement() {
        _uiState.value = AppUiState.NeedsApiKey(replacing = true)
    }

    fun cancelReplacement() {
        _uiState.value = AppUiState.Ready()
    }

    fun removeApiKey() {
        viewModelScope.launch {
            syncManager.cancel()
            when (secureApiKeyStorage.remove()) {
                is SecureApiKeyResult.Success -> _uiState.value = AppUiState.NeedsApiKey()
                is SecureApiKeyResult.Failure -> _uiState.value = AppUiState.Unavailable(
                    "Não foi possível remover completamente a credencial segura.",
                )
            }
        }
    }

    private fun inspectCredential() {
        viewModelScope.launch {
            _uiState.value = AppUiState.Loading
            _uiState.value = when (secureApiKeyStorage.status()) {
                SecureApiKeyStatus.NOT_CONFIGURED -> AppUiState.NeedsApiKey()
                SecureApiKeyStatus.RECOVERY_REQUIRED -> AppUiState.NeedsApiKey(
                    errorMessage = "A credencial anterior não pôde ser recuperada. Configure uma nova chave.",
                )
                SecureApiKeyStatus.UNAVAILABLE -> AppUiState.Unavailable(
                    "O armazenamento seguro não está disponível.",
                )
                SecureApiKeyStatus.CONFIGURED -> when (secureApiKeyStorage.read()) {
                    is SecureApiKeyResult.Success -> {
                        syncManager.start(
                            if (marketRepository.coinCount() == 0L) SyncTrigger.FIRST_RUN
                            else SyncTrigger.STARTUP_ESSENTIAL,
                        )
                        AppUiState.Ready()
                    }
                    is SecureApiKeyResult.Failure -> AppUiState.NeedsApiKey(
                        errorMessage = "A credencial não pôde ser descriptografada.",
                    )
                }
            }
        }
    }
}

