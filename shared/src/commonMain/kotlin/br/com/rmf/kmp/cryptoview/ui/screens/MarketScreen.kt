package br.com.rmf.kmp.cryptoview.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeSummary
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.ui.components.MarketTabs
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.PrimaryActionButton
import br.com.rmf.kmp.cryptoview.ui.components.RoundBrandLogo
import br.com.rmf.kmp.cryptoview.ui.components.RemoteBrandLogo
import br.com.rmf.kmp.cryptoview.ui.components.SparklineChart
import br.com.rmf.kmp.cryptoview.ui.components.SyncProgressContent
import br.com.rmf.kmp.cryptoview.ui.components.VariationPill
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.viewmodel.MarketViewModel
import org.koin.mp.KoinPlatformTools

private enum class MarketTab { Coins, Exchanges }

@Composable
fun MarketScreen(
    onExchangeClick: (Long) -> Unit,
    onCoinMarketsClick: (Long) -> Unit,
) {
    val marketViewModel = viewModel<MarketViewModel> {
        KoinPlatformTools.defaultContext().get().get<MarketViewModel>()
    }
    val state by marketViewModel.uiState.collectAsStateWithLifecycle()
    val sync by marketViewModel.syncState.collectAsStateWithLifecycle()
    var tabName by rememberSaveable { mutableStateOf(MarketTab.Coins.name) }
    var showSync by rememberSaveable { mutableStateOf(false) }
    val tab = MarketTab.valueOf(tabName)

    LaunchedEffect(sync.status) {
        if (sync.status == SyncStatus.RUNNING) showSync = true
    }
    DisposableEffect(Unit) { onDispose(marketViewModel::stopCoinDetails) }

    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mercado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Sincronizar",
                            modifier = Modifier.clickable {
                                marketViewModel.synchronize()
                                showSync = true
                            }.padding(10.dp),
                            color = CryptoOrange,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = marketViewModel::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar moeda ou corretora") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    MarketTabs(
                        selectedCoins = tab == MarketTab.Coins,
                        onCoinsClick = { tabName = MarketTab.Coins.name },
                        onExchangesClick = { tabName = MarketTab.Exchanges.name },
                    )
                }
            }

            if (tab == MarketTab.Coins) {
                if (state.coins.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyMarketMessage(sync.status == SyncStatus.RUNNING)
                }
                items(
                    state.coins,
                    key = { it.id },
                    span = { GridItemSpan(if (it.id == state.expandedCoinId) maxLineSpan else 1) },
                ) { coin ->
                    CoinCard(
                        coin = coin,
                        expanded = coin.id == state.expandedCoinId,
                        loading = state.detailsLoading,
                        history = state.expandedHistory.map { it.priceUsd.toFloat() },
                        marketNames = state.expandedMarkets.map { it.exchangeName }.distinct().take(4),
                        detailsMessage = state.historyError?.let { "Histórico indisponível neste plano." }
                            ?: state.marketsError?.let { "Corretoras indisponíveis neste plano." },
                        onClick = { marketViewModel.expandCoin(coin.id) },
                        onViewAll = { onCoinMarketsClick(coin.id) },
                    )
                }
            } else {
                if (state.exchanges.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyMarketMessage(sync.status == SyncStatus.RUNNING)
                }
                items(state.exchanges, key = { it.id }) { exchange ->
                    ExchangeCard(exchange) { onExchangeClick(exchange.id) }
                }
            }

            if ((if (tab == MarketTab.Coins) state.coins else state.exchanges).size >= state.limit) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PrimaryActionButton("Carregar mais", marketViewModel::loadMore, Modifier.fillMaxWidth())
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SyncProgressContent(
                        progress = sync,
                        onBackground = { showSync = false },
                        onCancel = marketViewModel::cancelSync,
                        onResume = marketViewModel::resumeSync,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMarketMessage(syncing: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (syncing) CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
        Text(
            if (syncing) "Preparando os dados do mercado…" else "Nenhum dado local. Inicie a sincronização.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CoinCard(
    coin: CoinSummary,
    expanded: Boolean,
    loading: Boolean,
    history: List<Float>,
    marketNames: List<String>,
    detailsMessage: String?,
    onClick: () -> Unit,
    onViewAll: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth().animateContentSize(), onClick) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteBrandLogo(coin.logoUrl, coin.symbol.take(2), brandColor(coin.id), Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(coin.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(coin.symbol, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatUsd(coin.priceUsd), fontWeight = FontWeight.SemiBold)
                    coin.percentChange24h?.let { VariationPill(it) }
                }
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (loading && history.isEmpty()) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    if (history.isNotEmpty()) SparklineChart(history)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric("Capitalização", formatCompactUsd(coin.marketCapUsd))
                        Metric("Volume 24h", formatCompactUsd(coin.volume24hUsd))
                        Metric("Ranking", coin.rank?.let { "#$it" } ?: "—")
                    }
                    if (marketNames.isNotEmpty()) Text(
                        "Corretoras: ${marketNames.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    detailsMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    Text(
                        "Ver todos os mercados",
                        modifier = Modifier.clickable(onClick = onViewAll).padding(vertical = 6.dp),
                        color = CryptoOrange,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExchangeCard(exchange: ExchangeSummary, onClick: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), onClick) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteBrandLogo(exchange.logoUrl, exchange.name.take(2).uppercase(), brandColor(exchange.id), Modifier.size(40.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(exchange.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${exchange.numMarketPairs ?: 0} mercados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCompactUsd(exchange.spotVolumeUsd), fontWeight = FontWeight.SemiBold)
                Text(exchange.rank?.let { "#$it" } ?: "", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

internal fun formatUsd(value: Double?): String = value?.let {
    val rounded = kotlin.math.round(it * 100.0) / 100.0
    "US$ $rounded"
} ?: "—"

internal fun formatCompactUsd(value: Double?): String = value?.let {
    when {
        it >= 1_000_000_000 -> "US$ ${kotlin.math.round(it / 10_000_000) / 100.0} bi"
        it >= 1_000_000 -> "US$ ${kotlin.math.round(it / 10_000) / 100.0} mi"
        it >= 1_000 -> "US$ ${kotlin.math.round(it / 10) / 100.0} mil"
        else -> formatUsd(it)
    }
} ?: "—"

private fun brandColor(id: Long): Color = listOf(
    Color(0xFFF4511E), Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A),
)[(id % 4).toInt()]
