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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.rmf.kmp.cryptoview.ui.components.InfoRow
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.PrimaryActionButton
import br.com.rmf.kmp.cryptoview.ui.components.SettingsDivider
import br.com.rmf.kmp.cryptoview.ui.components.SyncProgressContent
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoNegative
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange

@Composable
fun SettingsScreen(
    onReplaceKey: () -> Unit,
    onRemoveKey: () -> Unit,
) {
    var selectedCurrency by rememberSaveable { mutableStateOf("USD") }
    var keyStatus by rememberSaveable { mutableStateOf("Validada há 2 min") }
    var showSync by rememberSaveable { mutableStateOf(false) }
    var showClearCache by rememberSaveable { mutableStateOf(false) }

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
                Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Text("API CoinMarketCap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        InfoRow("Status", "●  Configurada")
                        InfoRow("Validação", keyStatus)
                        InfoRow("Plano", "Basic · mock")
                        InfoRow("Créditos usados", "1.248 de 15.000")
                        SettingsDivider()
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { keyStatus = "Validada agora" }, modifier = Modifier.weight(1f)) {
                                Text("Validar novamente")
                            }
                            OutlinedButton(onClick = onReplaceKey, modifier = Modifier.weight(1f)) {
                                Text("Substituir")
                            }
                        }
                        Text(
                            "Remover API key",
                            modifier = Modifier.clickable(onClick = onRemoveKey).padding(vertical = 8.dp),
                            color = CryptoNegative,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            item {
                Text("Moeda de exibição", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("USD", "BRL", "EUR").forEach { currency ->
                        OutlinedButton(
                            onClick = { selectedCurrency = currency },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(currency, color = if (selectedCurrency == currency) CryptoOrange else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            item {
                Text("Dados locais", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        InfoRow("Última sincronização", "Hoje, 09:40")
                        InfoRow("Moedas salvas", "1.000")
                        InfoRow("Corretoras salvas", "246")
                        Text(
                            "A moeda expandida atualiza a cada 60 segundos enquanto esta tela estiver ativa.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PrimaryActionButton("Sincronizar agora", { showSync = true }, Modifier.fillMaxWidth())
                        Text(
                            "Limpar cache local",
                            modifier = Modifier.clickable { showClearCache = true }.padding(vertical = 8.dp),
                            color = CryptoOrange,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        if (showSync) {
            Dialog(
                onDismissRequest = { showSync = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(.88f).widthIn(max = 440.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SyncProgressContent(
                        onBackground = { showSync = false },
                        onCancel = { showSync = false },
                    )
                }
            }
        }

        if (showClearCache) {
            AlertDialog(
                onDismissRequest = { showClearCache = false },
                title = { Text("Limpar cache local?") },
                text = { Text("Os dados mockados serão recarregados quando o mercado for aberto novamente.") },
                confirmButton = {
                    TextButton(onClick = { showClearCache = false }) { Text("Limpar", color = CryptoNegative) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCache = false }) { Text("Cancelar") }
                },
            )
        }
    }
}
