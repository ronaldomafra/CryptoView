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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rmf.kmp.cryptoview.ui.components.InfoRow
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.RoundBrandLogo
import br.com.rmf.kmp.cryptoview.ui.model.MockExchanges
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange

@Composable
fun ExchangeDetailScreen(exchangeId: Int, onBack: () -> Unit) {
    val exchange = MockExchanges.firstOrNull { it.id == exchangeId } ?: MockExchanges.first()
    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 900.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", modifier = Modifier.clickable(onClick = onBack).padding(8.dp), fontSize = 28.sp)
                    Text(
                        "Detalhe da corretora",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Voltar",
                        modifier = Modifier.clickable(onClick = onBack).padding(10.dp),
                        color = CryptoOrange,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoundBrandLogo(exchange.shortName, exchange.color, Modifier.size(58.dp))
                            Spacer(Modifier.width(13.dp))
                            Column {
                                Text(exchange.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("#${exchange.rank} por volume spot", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(exchange.description, style = MaterialTheme.typography.bodyMedium)
                        InfoRow("Volume 24h", exchange.volume24h)
                        InfoRow("Moedas disponíveis", exchange.coinCount.toString())
                        InfoRow("Lançamento", exchange.launchDate)
                        InfoRow("Taxa maker", exchange.makerFee)
                        InfoRow("Taxa taker", exchange.takerFee)
                        InfoRow("Website", exchange.website)
                    }
                }
            }
            item {
                Text("Ativos da corretora", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Saldos públicos informados pela corretora.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(exchange.assets, key = { it.symbol }) { asset ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        RoundBrandLogo(asset.symbol.take(2), exchange.color, Modifier.size(40.dp))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(asset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(asset.symbol, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(asset.amount, fontWeight = FontWeight.SemiBold)
                            Text(asset.value, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
