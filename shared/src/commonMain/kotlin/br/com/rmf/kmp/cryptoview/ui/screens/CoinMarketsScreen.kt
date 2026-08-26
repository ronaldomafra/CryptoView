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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.LocalFloatingNavigationContentPadding
import br.com.rmf.kmp.cryptoview.ui.components.CryptoIcon
import br.com.rmf.kmp.cryptoview.ui.components.RemoteBrandLogo
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.utils.formatUsd
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
    val floatingNavigationPadding = LocalFloatingNavigationContentPadding.current
    LaunchedEffect(coinId) { marketsViewModel.load(coinId) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).widthIn(max = 820.dp),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 22.dp,
            end = 18.dp,
            bottom = 26.dp + floatingNavigationPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    CryptoIcon(CryptoIcon.ChevronRight, "Voltar", Modifier.size(23.dp).rotate(180f), CryptoOrange)
                }
                Column {
                    Text("Onde comprar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.coin?.name ?: "Moeda", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (state.loading && state.markets.isEmpty()) item { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
        state.error?.let { item { Text("Mercados indisponíveis para o plano atual.", color = MaterialTheme.colorScheme.error) } }
        items(state.markets, key = { "${it.exchangeId}:${it.marketPair}" }) { market ->
            OutlinedCard(Modifier.fillMaxWidth(), onClick = { onExchangeClick(market.exchangeId) }) {
                Row(Modifier.heightIn(min = 90.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteBrandLogo(market.exchangeLogoUrl, market.exchangeName.take(2).uppercase(), Color(0xFFF4511E), Modifier.size(48.dp))
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(market.exchangeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${market.marketPair} · ${market.category ?: "Mercado"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatUsd(market.priceUsd), fontWeight = FontWeight.SemiBold)
                        Text("Ver corretora", color = CryptoOrange, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
