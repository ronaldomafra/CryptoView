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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import br.com.rmf.kmp.cryptoview.utils.TempUtils
import org.koin.mp.KoinPlatformTools

@Composable
fun CoinMarketCapTestScreen() {
    val testViewModel = viewModel<CoinMarketCapTestViewModel> {
        KoinPlatformTools.defaultContext().get().get<CoinMarketCapTestViewModel>()
    }
    val state by testViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = state is CoinMarketCapTestUiState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Testes da API",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tela temporária para validar a integração com a CoinMarketCap.",
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
                        Text(
                            text = "GET /v1/key/info",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        InfoRow("Origem da chave", "TempUtils.API_KEY")
                        InfoRow("Chave utilizada", TempUtils.API_KEY.maskedApiKey())
                        SettingsDivider()
                        PrimaryActionButton(
                            text = "Consultar informações da chave",
                            onClick = {
                                testViewModel.executeKeyInfoTest(
                                    testName = "Consulta GET /v1/key/info",
                                    apiKey = TempUtils.API_KEY,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                        )
                        OutlinedButton(
                            onClick = {
                                testViewModel.executeKeyInfoTest(
                                    testName = "Validação local sem chave",
                                    apiKey = "",
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                        ) {
                            Text("Testar parâmetro inválido")
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
                    TestMessageCard(
                        title = "Nenhum teste executado",
                        message = "Use um dos botões acima para visualizar o retorno.",
                    )
                }

                is CoinMarketCapTestUiState.Loading -> item {
                    LoadingCard(currentState.testName)
                }

                is CoinMarketCapTestUiState.Completed -> item {
                    when (val result = currentState.result) {
                        is ApiResult.Success -> SuccessResult(
                            testName = currentState.testName,
                            success = result,
                        )

                        is ApiResult.Failure -> FailureResult(
                            testName = currentState.testName,
                            failure = result,
                        )
                    }
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
                    text = "Temporário: remova esta tela e a chave fixa antes de distribuir o aplicativo.",
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
private fun SuccessResult(
    testName: String,
    success: ApiResult.Success<CoinMarketCapKeyInfo>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        TestMessageCard(
            title = "Sucesso",
            message = "A API respondeu e o conteúdo foi convertido para o modelo de domínio.",
            statusColor = CryptoPositive,
            testName = testName,
        )

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Metadados da resposta", style = MaterialTheme.typography.titleLarge)
                InfoRow("Timestamp", success.metadata.timestamp.displayValue())
                InfoRow("Tempo da API", success.metadata.elapsed?.let { "$it ms" }.displayValue())
                InfoRow("Créditos consumidos", success.metadata.creditCount.displayValue())
                InfoRow("Aviso", success.metadata.notice.displayValue())
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            val plan = success.data.plan
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Plano", style = MaterialTheme.typography.titleLarge)
                InfoRow("Limite mensal", plan?.creditLimitMonthly.displayValue())
                InfoRow("Limite por minuto", plan?.rateLimitMinute.displayValue())
                InfoRow("Regra de renovação", plan?.creditLimitMonthlyReset.displayValue())
                InfoRow("Próxima renovação", plan?.creditLimitMonthlyResetTimestamp.displayValue())
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            val usage = success.data.usage
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Uso", style = MaterialTheme.typography.titleLarge)
                InfoRow("Requisições no minuto", usage?.currentMinute?.requestsMade.displayValue())
                InfoRow("Restantes no minuto", usage?.currentMinute?.requestsLeft.displayValue())
                InfoRow("Créditos usados no dia", usage?.currentDay?.creditsUsed.displayValue())
                InfoRow("Créditos restantes no dia", usage?.currentDay?.creditsLeft.displayValue())
                InfoRow("Créditos usados no mês", usage?.currentMonth?.creditsUsed.displayValue())
                InfoRow("Créditos restantes no mês", usage?.currentMonth?.creditsLeft.displayValue())
            }
        }

        RawResultCard("Objeto retornado", "data=${success.data}\nmetadata=${success.metadata}")
    }
}

@Composable
private fun FailureResult(
    testName: String,
    failure: ApiResult.Failure,
) {
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
            Text(title, style = MaterialTheme.typography.titleLarge, color = statusColor)
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
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(9.dp))
            SelectionContainer {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
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

private fun String.maskedApiKey(): String = when {
    isEmpty() -> "Não configurada"
    length <= API_KEY_VISIBLE_SUFFIX -> "••••"
    else -> "••••••••${takeLast(API_KEY_VISIBLE_SUFFIX)}"
}

private fun Any?.displayValue(): String = this?.toString() ?: "—"

private const val API_KEY_VISIBLE_SUFFIX = 4
