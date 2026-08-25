package br.com.rmf.kmp.cryptoview.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rmf.kmp.cryptoview.ui.components.InfoRow
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.RoundBrandLogo
import br.com.rmf.kmp.cryptoview.ui.components.RemoteBrandLogo
import br.com.rmf.kmp.cryptoview.ui.viewmodel.ExchangeDetailViewModel
import org.koin.mp.KoinPlatformTools

@Composable
fun ExchangeDetailScreen(exchangeId: Long, onBack: () -> Unit) {
    val detailViewModel = viewModel<ExchangeDetailViewModel> {
        KoinPlatformTools.defaultContext().get().get<ExchangeDetailViewModel>()
    }
    val state by detailViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(exchangeId) { detailViewModel.load(exchangeId) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).widthIn(max = 900.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", modifier = Modifier.clickable(onClick = onBack).padding(8.dp), style = MaterialTheme.typography.headlineMedium)
                Text("Detalhe da corretora", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        state.exchange?.let { exchange ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RemoteBrandLogo(exchange.logoUrl, exchange.name.take(2).uppercase(), Color(0xFFF4511E), Modifier.size(46.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(exchange.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(exchange.rank?.let { "Ranking #$it" } ?: "Ranking indisponível", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        exchange.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        InfoRow("Volume spot 24h", formatCompactUsd(exchange.spotVolumeUsd))
                        InfoRow("Mercados", exchange.numMarketPairs?.toString() ?: "—")
                        InfoRow("Lançamento", exchange.dateLaunched?.take(10) ?: "—")
                        InfoRow("Taxa maker", exchange.makerFee?.let { "$it%" } ?: "—")
                        InfoRow("Taxa taker", exchange.takerFee?.let { "$it%" } ?: "—")
                        InfoRow("Website", exchange.websiteUrl ?: "—")
                    }
                }
            }
        }
        item {
            Text("Ativos da corretora", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (state.loadingAssets) CircularProgressIndicator(Modifier.padding(top = 12.dp).size(24.dp), strokeWidth = 2.dp)
            state.error?.let { Text("Ativos indisponíveis para o plano atual.", color = MaterialTheme.colorScheme.error) }
        }
        items(state.assets, key = { it.currencyId }) { asset ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundBrandLogo(asset.symbol.take(2), Color(0xFF1565C0), Modifier.size(38.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(asset.name, fontWeight = FontWeight.SemiBold)
                        Text(asset.symbol, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(asset.balance?.toString() ?: "—", fontWeight = FontWeight.SemiBold)
                        Text(formatUsd(asset.valueUsd), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
