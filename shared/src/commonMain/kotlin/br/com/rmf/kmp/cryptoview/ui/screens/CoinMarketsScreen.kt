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
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.RoundBrandLogo
import br.com.rmf.kmp.cryptoview.ui.components.RemoteBrandLogo
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.viewmodel.CoinMarketsViewModel
import org.koin.mp.KoinPlatformTools

@Composable
fun CoinMarketsScreen(
    coinId: Long,
    onBack: () -> Unit,
    onExchangeClick: (Long) -> Unit,
) {
    val marketsViewModel = viewModel<CoinMarketsViewModel> {
        KoinPlatformTools.defaultContext().get().get<CoinMarketsViewModel>()
    }
    val state by marketsViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(coinId) { marketsViewModel.load(coinId) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).widthIn(max = 820.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", modifier = Modifier.clickable(onClick = onBack).padding(8.dp), style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text("Onde comprar ${state.coin?.name ?: "moeda"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Mercados carregados por demanda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.loading && state.markets.isEmpty()) item { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
        state.error?.let { item { Text("Mercados indisponíveis para o plano atual.", color = MaterialTheme.colorScheme.error) } }
        items(state.markets, key = { "${it.exchangeId}:${it.marketPair}" }) { market ->
            OutlinedCard(Modifier.fillMaxWidth(), onClick = { onExchangeClick(market.exchangeId) }) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteBrandLogo(market.exchangeLogoUrl, market.exchangeName.take(2).uppercase(), Color(0xFFF4511E), Modifier.size(40.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(market.exchangeName, fontWeight = FontWeight.SemiBold)
                        Text("${market.marketPair} · ${market.category ?: "Mercado"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatUsd(market.priceUsd), fontWeight = FontWeight.SemiBold)
                        Text("Detalhes ›", color = CryptoOrange, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
