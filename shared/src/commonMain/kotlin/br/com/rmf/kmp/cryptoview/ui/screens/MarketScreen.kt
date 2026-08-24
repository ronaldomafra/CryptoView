package br.com.rmf.kmp.cryptoview.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.rmf.kmp.cryptoview.ui.components.ExchangeBadges
import br.com.rmf.kmp.cryptoview.ui.components.MarketTabs
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.PrimaryActionButton
import br.com.rmf.kmp.cryptoview.ui.components.RoundBrandLogo
import br.com.rmf.kmp.cryptoview.ui.components.SparklineChart
import br.com.rmf.kmp.cryptoview.ui.components.SyncProgressContent
import br.com.rmf.kmp.cryptoview.ui.components.VariationPill
import br.com.rmf.kmp.cryptoview.ui.model.MockCoin
import br.com.rmf.kmp.cryptoview.ui.model.MockCoins
import br.com.rmf.kmp.cryptoview.ui.model.MockExchange
import br.com.rmf.kmp.cryptoview.ui.model.MockExchanges
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoBorder
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrangeSoft

private enum class MarketTab { Coins, Exchanges }
private enum class SortMode { MarketCap, Price }
private enum class VariationFilter { All, Positive, Negative }

@Composable
fun MarketScreen(
    onExchangeClick: (Int) -> Unit,
    onCoinMarketsClick: (Int) -> Unit,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(MarketTab.Coins.name) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var sortModeName by rememberSaveable { mutableStateOf(SortMode.MarketCap.name) }
    var variationName by rememberSaveable { mutableStateOf(VariationFilter.All.name) }
    var exchangeFilter by rememberSaveable { mutableStateOf("Todas") }
    var expandedCoinId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSync by rememberSaveable { mutableStateOf(false) }

    val selectedTab = MarketTab.valueOf(selectedTabName)
    val sortMode = SortMode.valueOf(sortModeName)
    val variation = VariationFilter.valueOf(variationName)
    val filteredCoins = remember(query, sortMode, variation, exchangeFilter) {
        MockCoins
            .asSequence()
            .filter { query.isBlank() || it.name.contains(query, true) || it.symbol.contains(query, true) }
            .filter {
                when (variation) {
                    VariationFilter.All -> true
                    VariationFilter.Positive -> it.variation24h >= 0
                    VariationFilter.Negative -> it.variation24h < 0
                }
            }
            .filter { coin -> exchangeFilter == "Todas" || coin.exchanges.any { it.name == exchangeFilter } }
            .let { sequence ->
                when (sortMode) {
                    SortMode.MarketCap -> sequence.sortedBy { it.marketCapRank }
                    SortMode.Price -> sequence.sortedByDescending { it.priceValue }
                }
            }
            .toList()
    }
    val filteredExchanges = remember(query) {
        MockExchanges.filter { query.isBlank() || it.name.contains(query, true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MarketHeader(
                    searchOpen = searchOpen,
                    query = query,
                    filtersOpen = filtersOpen,
                    onSearchToggle = {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
                    },
                    onQueryChange = { query = it },
                    onFiltersToggle = { filtersOpen = !filtersOpen },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                MarketTabs(
                    selectedCoins = selectedTab == MarketTab.Coins,
                    onCoinsClick = { selectedTabName = MarketTab.Coins.name },
                    onExchangesClick = { selectedTabName = MarketTab.Exchanges.name },
                )
            }
            if (filtersOpen && selectedTab == MarketTab.Coins) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterPanel(
                        sortMode = sortMode,
                        variation = variation,
                        exchange = exchangeFilter,
                        onSortChange = { sortModeName = it.name },
                        onVariationChange = { variationName = it.name },
                        onExchangeChange = { exchangeFilter = it },
                        onClear = {
                            sortModeName = SortMode.MarketCap.name
                            variationName = VariationFilter.All.name
                            exchangeFilter = "Todas"
                        },
                        onApply = { filtersOpen = false },
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSync = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("↻", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                    Text(
                        if (query.isBlank()) "Atualizado há 1 min" else "${if (selectedTab == MarketTab.Coins) filteredCoins.size else filteredExchanges.size} resultado(s)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (selectedTab == MarketTab.Coins) {
                items(
                    items = filteredCoins,
                    key = { it.id },
                    span = { coin -> GridItemSpan(if (coin.id == expandedCoinId) maxLineSpan else 1) },
                ) { coin ->
                    CoinCard(
                        coin = coin,
                        expanded = coin.id == expandedCoinId,
                        onClick = { expandedCoinId = if (expandedCoinId == coin.id) null else coin.id },
                        onViewAll = { onCoinMarketsClick(coin.id) },
                    )
                }
            } else {
                items(filteredExchanges, key = { it.id }) { exchange ->
                    ExchangeCard(exchange = exchange, onClick = { onExchangeClick(exchange.id) })
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
    }
}

@Composable
private fun MarketHeader(
    searchOpen: Boolean,
    query: String,
    filtersOpen: Boolean,
    onSearchToggle: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFiltersToggle: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Mercado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearchToggle) { Text(if (searchOpen) "×" else "⌕", fontSize = 22.sp) }
            IconButton(
                onClick = onFiltersToggle,
                modifier = if (filtersOpen) Modifier.background(CryptoOrangeSoft, CircleShape) else Modifier,
            ) { Text("☷", color = if (filtersOpen) CryptoOrange else MaterialTheme.colorScheme.onSurface, fontSize = 20.sp) }
        }
        AnimatedVisibility(searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar moeda ou corretora") },
                leadingIcon = { Text("⌕", fontSize = 20.sp) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) { Text("×", fontSize = 20.sp) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

@Composable
private fun FilterPanel(
    sortMode: SortMode,
    variation: VariationFilter,
    exchange: String,
    onSortChange: (SortMode) -> Unit,
    onVariationChange: (VariationFilter) -> Unit,
    onExchangeChange: (String) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Filtros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Ordenar por", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceButton("Capitalização", sortMode == SortMode.MarketCap, { onSortChange(SortMode.MarketCap) }, Modifier.weight(1f))
                ChoiceButton("Preço", sortMode == SortMode.Price, { onSortChange(SortMode.Price) }, Modifier.weight(1f))
            }
            Text("Variação 24h", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceButton("Todas", variation == VariationFilter.All, { onVariationChange(VariationFilter.All) }, Modifier.weight(1f))
                ChoiceButton("Positivas", variation == VariationFilter.Positive, { onVariationChange(VariationFilter.Positive) }, Modifier.weight(1f))
                ChoiceButton("Negativas", variation == VariationFilter.Negative, { onVariationChange(VariationFilter.Negative) }, Modifier.weight(1f))
            }
            Text("Corretora", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todas", "Binance", "Coinbase").forEach { option ->
                    ChoiceButton(option, exchange == option, { onExchangeChange(option) }, Modifier.weight(1f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Limpar",
                    modifier = Modifier.clickable(onClick = onClear).padding(12.dp),
                    color = CryptoOrange,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                PrimaryActionButton("Aplicar", onApply, Modifier.width(140.dp))
            }
        }
    }
}

@Composable
private fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) CryptoOrange else CryptoBorder),
    ) {
        Text(
            text = text,
            color = if (selected) CryptoOrange else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun CoinCard(
    coin: MockCoin,
    expanded: Boolean,
    onClick: () -> Unit,
    onViewAll: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        onClick = onClick,
    ) {
        if (expanded) ExpandedCoinContent(coin, onViewAll) else CompactCoinContent(coin)
    }
}

@Composable
private fun CompactCoinContent(coin: MockCoin) {
    Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundBrandLogo(coin.glyph, coin.brandColor, Modifier.size(46.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(.8f)) {
            Text(
                coin.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(coin.symbol, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(
            modifier = Modifier.weight(1.05f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(coin.price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            ExchangeBadges(coin.exchanges, coin.additionalExchanges)
        }
        Spacer(Modifier.width(8.dp))
        VariationPill(coin.variation24h)
        Text("›", modifier = Modifier.padding(start = 6.dp), fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CoinIdentity(coin: MockCoin, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RoundBrandLogo(coin.glyph, coin.brandColor, Modifier.size(48.dp))
        Spacer(Modifier.width(11.dp))
        Column {
            Text(coin.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(coin.symbol, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExpandedCoinContent(coin: MockCoin, onViewAll: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinIdentity(coin, Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(coin.price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                VariationPill(coin.variation24h)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("↻  Atualizado há 12 s · atualiza a cada 60 s", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        Text("Últimas 24 horas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SparklineChart(coin.chart, Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("Mín.", coin.minimum, Modifier.weight(1f))
            Metric("Máx.", coin.maximum, Modifier.weight(1f))
            Metric("Volume 24h", coin.volume24h, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(CryptoBorder))
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Negociada em", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                "Ver todas",
                modifier = Modifier.clickable(onClick = onViewAll).padding(8.dp),
                color = CryptoOrange,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        ExchangeBadges(coin.exchanges, 0, showNames = true)
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExchangeCard(exchange: MockExchange, onClick: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundBrandLogo(exchange.shortName, exchange.color, Modifier.size(48.dp))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(exchange.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("#${exchange.rank}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Volume 24h", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(exchange.volume24h, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${exchange.coinCount} moedas", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", modifier = Modifier.padding(start = 8.dp), fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
