package br.com.rmf.kmp.cryptoview.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.ui.components.InfoRow
import br.com.rmf.kmp.cryptoview.ui.components.LocalFloatingNavigationContentPadding
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.PrimaryActionButton
import br.com.rmf.kmp.cryptoview.ui.components.SettingsDivider
import br.com.rmf.kmp.cryptoview.ui.components.SyncRunningAction
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoNegative
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.viewmodel.SettingsViewModel
import org.koin.mp.KoinPlatformTools

@Composable
fun SettingsScreen(
    keyInfo: CoinMarketCapKeyInfo?,
    validationMessage: String?,
    onRevalidateKey: () -> Unit,
    onReplaceKey: () -> Unit,
    onRemoveKey: () -> Unit,
    syncDialogVisible: Boolean,
    onShowSync: () -> Unit,
) {
    val settingsViewModel = viewModel<SettingsViewModel> {
        KoinPlatformTools.defaultContext().get().get<SettingsViewModel>()
    }
    val data by settingsViewModel.dataState.collectAsStateWithLifecycle()
    val sync by settingsViewModel.syncState.collectAsStateWithLifecycle()
    var showClear by rememberSaveable { mutableStateOf(false) }
    var showRemove by rememberSaveable { mutableStateOf(false) }
    val floatingNavigationPadding = LocalFloatingNavigationContentPadding.current

    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 820.dp),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 22.dp,
                end = 18.dp,
                bottom = 26.dp + floatingNavigationPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ajustes", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.weight(1f))
                    if (sync.status == SyncStatus.RUNNING && !syncDialogVisible) {
                        SyncRunningAction(onClick = onShowSync)
                    }
                }
            }
            item {
                Text("API CoinMarketCap", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        InfoRow("Status", "Configurada com segurança")
                        validationMessage?.let { InfoRow("Validação", it) }
                        InfoRow("Limite mensal", keyInfo?.plan?.creditLimitMonthly?.toString() ?: "Consultar novamente")
                        InfoRow("Créditos usados", keyInfo?.usage?.currentMonth?.creditsUsed?.toString() ?: "—")
                        InfoRow("Créditos restantes", keyInfo?.usage?.currentMonth?.creditsLeft?.toString() ?: "—")
                        InfoRow("Requisições/min", keyInfo?.plan?.rateLimitMinute?.toString() ?: "—")
                        SettingsDivider()
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onRevalidateKey, modifier = Modifier.weight(1f)) { Text("Validar") }
                            OutlinedButton(onClick = onReplaceKey, modifier = Modifier.weight(1f)) { Text("Substituir") }
                        }
                        Text(
                            "Remover API key",
                            modifier = Modifier.clickable { showRemove = true }.padding(vertical = 8.dp),
                            color = CryptoNegative,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            item {
                Text("Dados locais", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        InfoRow("Moedas salvas", data.coinCount.toString())
                        InfoRow("Corretoras salvas", data.exchangeCount.toString())
                        InfoRow("Estado", sync.message ?: "Pronto")
                        Text(
                            "O histórico e os mercados são baixados somente ao abrir uma moeda. A cotação aberta é atualizada a cada 60 segundos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PrimaryActionButton(
                            "Sincronizar agora",
                            onClick = {
                                if (sync.status != SyncStatus.RUNNING) settingsViewModel.synchronize()
                                onShowSync()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            if (data.clearing) "Limpando…" else "Limpar cache local",
                            modifier = Modifier.clickable(enabled = !data.clearing) { showClear = true }.padding(vertical = 8.dp),
                            color = CryptoOrange,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

    }

    if (showClear) ConfirmDialog(
        title = "Limpar cache local?",
        message = "Moedas, corretoras, históricos e checkpoints serão removidos. A API key protegida será mantida.",
        confirm = "Limpar",
        onConfirm = {
            showClear = false
            settingsViewModel.clearCache()
        },
        onDismiss = { showClear = false },
    )
    if (showRemove) ConfirmDialog(
        title = "Remover API key?",
        message = "A credencial protegida será removida deste dispositivo.",
        confirm = "Remover",
        onConfirm = {
            showRemove = false
            onRemoveKey()
        },
        onDismiss = { showRemove = false },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(message) },
    confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = CryptoNegative) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
)
