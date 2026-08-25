package br.com.rmf.kmp.cryptoview.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rmf.kmp.cryptoview.domain.model.ApiResult
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo
import br.com.rmf.kmp.cryptoview.domain.model.CryptoError
import br.com.rmf.kmp.cryptoview.ui.components.InfoRow
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.PrimaryActionButton
import br.com.rmf.kmp.cryptoview.ui.components.SettingsDivider
import br.com.rmf.kmp.cryptoview.ui.model.CoinMarketCapTestUiState
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoNegative
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoPositive
import br.com.rmf.kmp.cryptoview.ui.viewmodel.CoinMarketCapTestViewModel
import org.koin.mp.KoinPlatformTools

@Composable
fun CoinMarketCapTestScreen() {
    val testViewModel = viewModel<CoinMarketCapTestViewModel> {
        KoinPlatformTools.defaultContext().get().get<CoinMarketCapTestViewModel>()
    }
    val state by testViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = state is CoinMarketCapTestUiState.Loading
    var apiKey by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Teste do armazenamento seguro",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Valide a API key, grave o envelope criptografado e teste sua recuperação.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Text("CoinMarketCap API key", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API key") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                        )
                        Text(
                            text = "O texto puro não é exibido no resultado nem persistido no DataStore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PrimaryActionButton(
                            text = "Validar e salvar com segurança",
                            onClick = {
                                val candidate = apiKey
                                apiKey = ""
                                testViewModel.validateAndSave(candidate)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && apiKey.isNotBlank(),
                        )
                        SettingsDivider()
                        OutlinedButton(
                            onClick = testViewModel::readAndValidate,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                        ) {
                            Text("Ler, descriptografar e validar")
                        }
                        OutlinedButton(
                            onClick = testViewModel::checkStorageStatus,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                        ) {
                            Text("Consultar status")
                        }
                        OutlinedButton(
                            onClick = testViewModel::removeStoredApiKey,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                        ) {
                            Text("Remover chave armazenada")
                        }
                        TextButton(
                            onClick = testViewModel::clearResult,
                            modifier = Modifier.align(Alignment.End),
                            enabled = state !is CoinMarketCapTestUiState.Idle,
                        ) {
                            Text("Limpar retorno", color = CryptoOrange)
                        }
                    }
                }
            }

            when (val currentState = state) {
                CoinMarketCapTestUiState.Idle -> item {
                    TestMessageCard("Nenhum teste executado", "Escolha uma operação acima.")
                }
                is CoinMarketCapTestUiState.Loading -> item { LoadingCard(currentState.testName) }
                is CoinMarketCapTestUiState.Completed -> item {
                    currentState.storageMessage?.let { message ->
                        TestMessageCard(
                            title = "Armazenamento seguro validado",
                            message = message,
                            statusColor = CryptoPositive,
                            testName = currentState.testName,
                        )
                        Spacer(Modifier.height(13.dp))
                    }
                    when (val result = currentState.result) {
                        is ApiResult.Success -> SuccessResult(currentState.testName, result)
                        is ApiResult.Failure -> FailureResult(currentState.testName, result)
                    }
                }
                is CoinMarketCapTestUiState.StorageCompleted -> item {
                    TestMessageCard(
                        title = if (currentState.successful) "Operação concluída" else "Falha",
                        message = currentState.message,
                        statusColor = if (currentState.successful) CryptoPositive else CryptoNegative,
                        testName = currentState.testName,
                    )
                }
                is CoinMarketCapTestUiState.UnexpectedFailure -> item {
                    TestMessageCard(
                        title = "Falha inesperada",
                        message = currentState.detail ?: "Erro sem detalhes",
                        statusColor = CryptoNegative,
                        testName = currentState.testName,
                    )
                }
            }

            item {
                Text(
                    text = "Tela temporária de validação; será removida antes da distribuição.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadingCard(testName: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator()
            Column {
                Text("Executando teste", fontWeight = FontWeight.Bold)
                Text(testName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SuccessResult(testName: String, success: ApiResult.Success<CoinMarketCapKeyInfo>) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        TestMessageCard(
            title = "Sucesso",
            message = "A API aceitou a chave e retornou as informações da conta.",
            statusColor = CryptoPositive,
            testName = testName,
        )
        OutlinedCard(Modifier.fillMaxWidth()) {
            val plan = success.data.plan
            val usage = success.data.usage
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Resumo seguro da resposta", style = MaterialTheme.typography.titleMedium)
                InfoRow("Timestamp", success.metadata.timestamp.displayValue())
                InfoRow("Créditos consumidos", success.metadata.creditCount.displayValue())
                InfoRow("Limite mensal", plan?.creditLimitMonthly.displayValue())
                InfoRow("Créditos usados no mês", usage?.currentMonth?.creditsUsed.displayValue())
            }
        }
    }
}

@Composable
private fun FailureResult(testName: String, failure: ApiResult.Failure) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        TestMessageCard(
            title = "Falha",
            message = failure.error.displayMessage(),
            statusColor = CryptoNegative,
            testName = testName,
        )
        RawResultCard("Objeto retornado", failure.toString())
    }
}

@Composable
private fun TestMessageCard(
    title: String,
    message: String,
    statusColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    testName: String? = null,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = statusColor)
            testName?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RawResultCard(title: String, value: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(9.dp))
            SelectionContainer {
                Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

private fun CryptoError.displayMessage(): String = when (this) {
    CryptoError.NoConnection -> "Sem conexão com a internet."
    CryptoError.Timeout -> "A requisição excedeu o tempo limite."
    CryptoError.MissingApiKey -> "A chave da API não foi informada."
    is CryptoError.InvalidApiKey -> apiMessage ?: "A CoinMarketCap rejeitou a chave da API."
    is CryptoError.PlanUnavailable -> apiMessage ?: "O endpoint não está disponível para este plano."
    is CryptoError.RateLimited -> apiMessage ?: "O limite de requisições foi atingido."
    is CryptoError.ServerUnavailable -> "Servidor indisponível. HTTP $statusCode."
    is CryptoError.InvalidResponse -> detail ?: "A resposta recebida é inválida."
    is CryptoError.Serialization -> detail ?: "Não foi possível interpretar a resposta."
    is CryptoError.Unknown -> detail ?: "Erro desconhecido."
}

private fun Any?.displayValue(): String = this?.toString() ?: "—"
