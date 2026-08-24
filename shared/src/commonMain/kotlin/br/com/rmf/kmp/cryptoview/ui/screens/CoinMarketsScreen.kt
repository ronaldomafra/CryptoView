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
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.RoundBrandLogo
import br.com.rmf.kmp.cryptoview.ui.model.MockCoins
import br.com.rmf.kmp.cryptoview.ui.model.MockExchanges
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange

@Composable
fun CoinMarketsScreen(
    coinId: Int,
    onBack: () -> Unit,
    onExchangeClick: (Int) -> Unit,
) {
    val coin = MockCoins.firstOrNull { it.id == coinId } ?: MockCoins.first()
    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", fontSize = 28.sp, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Onde comprar ${coin.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Mercados disponíveis", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(MockExchanges, key = { it.id }) { exchange ->
                OutlinedCard(Modifier.fillMaxWidth(), onClick = { onExchangeClick(exchange.id) }) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        RoundBrandLogo(exchange.shortName, exchange.color, Modifier.size(44.dp))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(exchange.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${coin.symbol}/USDT · Spot", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(coin.price, fontWeight = FontWeight.SemiBold)
                            Text("Abrir detalhe ›", color = CryptoOrange, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
