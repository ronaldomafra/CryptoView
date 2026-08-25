package br.com.rmf.kmp.cryptoview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.repository.CoinMarketCapDataRepository
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyError
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyResult
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStatus
import br.com.rmf.kmp.cryptoview.security.SecureApiKeyStorage
import br.com.rmf.kmp.cryptoview.ui.model.CoinMarketCapTestUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CoinMarketCapTestViewModel internal constructor(
    private val repository: CoinMarketCapDataRepository,
    private val secureApiKeyStorage: SecureApiKeyStorage,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CoinMarketCapTestUiState>(
        CoinMarketCapTestUiState.Idle,
    )
    val uiState: StateFlow<CoinMarketCapTestUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null

    fun validateAndSave(apiKey: String) {
        launchOperation("Validar e salvar com segurança") {
            repository.getKeyInfo(apiKey).collect { result ->
                if (result is ApiResult.Success) {
                    when (val saveResult = secureApiKeyStorage.save(apiKey)) {
                        is SecureApiKeyResult.Success -> {
                            _uiState.value = CoinMarketCapTestUiState.Completed(
                                testName = "Validar e salvar com segurança",
                                result = result,
                                storageMessage = "Chave validada, criptografada e salva com segurança.",
                            )
                        }

                        is SecureApiKeyResult.Failure -> showStorageFailure(
                            testName = "Salvar chave com segurança",
                            error = saveResult.error,
                        )
                    }
                } else {
                    _uiState.value = CoinMarketCapTestUiState.Completed(
                        testName = "Validar antes de salvar",
                        result = result,
                    )
                }
            }
        }
    }

    fun readAndValidate() {
        launchOperation("Ler, descriptografar e validar") {
            when (val readResult = secureApiKeyStorage.read()) {
                is SecureApiKeyResult.Success -> {
                    repository.getKeyInfo(readResult.value).collect { result ->
                        _uiState.value = CoinMarketCapTestUiState.Completed(
                            testName = "Chave recuperada do cofre e validada",
                            result = result,
                            storageMessage = if (result is ApiResult.Success) {
                                "A chave foi descriptografada somente em memória e aceita pela API."
                            } else {
                                null
                            },
                        )
                    }
                }

                is SecureApiKeyResult.Failure -> showStorageFailure(
                    testName = "Ler chave segura",
                    error = readResult.error,
                )
            }
        }
    }

    fun checkStorageStatus() {
        launchOperation("Consultar status do cofre") {
            val status = secureApiKeyStorage.status()
            _uiState.value = CoinMarketCapTestUiState.StorageCompleted(
                testName = "Status do armazenamento seguro",
                message = status.displayMessage(),
                successful = status == SecureApiKeyStatus.CONFIGURED ||
                    status == SecureApiKeyStatus.NOT_CONFIGURED,
            )
        }
    }

    fun removeStoredApiKey() {
        launchOperation("Remover chave armazenada") {
            when (val result = secureApiKeyStorage.remove()) {
                is SecureApiKeyResult.Success -> {
                    _uiState.value = CoinMarketCapTestUiState.StorageCompleted(
                        testName = "Remover chave armazenada",
                        message = "Envelope removido e chave criptográfica excluída do cofre nativo.",
                        successful = true,
                    )
                }

                is SecureApiKeyResult.Failure -> showStorageFailure(
                    testName = "Remover chave armazenada",
                    error = result.error,
                )
            }
        }
    }

    fun clearResult() {
        requestJob?.cancel()
        requestJob = null
        _uiState.value = CoinMarketCapTestUiState.Idle
    }

    private fun launchOperation(testName: String, block: suspend () -> Unit) {
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            _uiState.value = CoinMarketCapTestUiState.Loading(testName)
            try {
                block()
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

    private fun showStorageFailure(testName: String, error: SecureApiKeyError) {
        _uiState.value = CoinMarketCapTestUiState.StorageCompleted(
            testName = testName,
            message = error.displayMessage(),
            successful = false,
        )
    }
}

private fun SecureApiKeyStatus.displayMessage(): String = when (this) {
    SecureApiKeyStatus.NOT_CONFIGURED -> "Nenhuma chave está armazenada."
    SecureApiKeyStatus.CONFIGURED -> "Existe um envelope criptografado pronto para leitura."
    SecureApiKeyStatus.RECOVERY_REQUIRED -> "O envelope está incompleto ou usa uma versão incompatível."
    SecureApiKeyStatus.UNAVAILABLE -> "Não foi possível consultar o armazenamento seguro."
}

private fun SecureApiKeyError.displayMessage(): String = when (this) {
    SecureApiKeyError.INVALID_INPUT -> "Informe uma API key antes de salvar."
    SecureApiKeyError.NOT_CONFIGURED -> "Nenhuma API key está armazenada."
    SecureApiKeyError.ENCRYPTION_FAILED -> "Não foi possível criptografar a API key."
    SecureApiKeyError.DECRYPTION_FAILED ->
        "Não foi possível descriptografar a API key; os dados inválidos foram removidos."
    SecureApiKeyError.PERSISTENCE_FAILED -> "Falha ao acessar o envelope criptografado."
    SecureApiKeyError.KEY_DELETION_FAILED ->
        "O envelope foi removido, mas a chave nativa não pôde ser excluída."
}
