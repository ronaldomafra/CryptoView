package br.com.rmf.kmp.cryptoview.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rmf.kmp.cryptoview.currentTimeMillis
import br.com.rmf.kmp.cryptoview.domain.model.CoinExchangeMarket
import br.com.rmf.kmp.cryptoview.domain.model.CoinSortOrder
import br.com.rmf.kmp.cryptoview.domain.model.CoinSummary
import br.com.rmf.kmp.cryptoview.domain.model.CoinVariationFilter
import br.com.rmf.kmp.cryptoview.domain.model.ExchangeSummary
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.ui.components.CryptoIcon
import br.com.rmf.kmp.cryptoview.ui.components.MarketTabs
import br.com.rmf.kmp.cryptoview.ui.components.LocalFloatingNavigationContentPadding
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.PrimaryActionButton
import br.com.rmf.kmp.cryptoview.ui.components.RemoteBrandLogo
import br.com.rmf.kmp.cryptoview.ui.components.SparklineChart
import br.com.rmf.kmp.cryptoview.ui.components.SyncProgressContent
import br.com.rmf.kmp.cryptoview.ui.components.VariationPill
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoBorder
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrangeSoft
import br.com.rmf.kmp.cryptoview.ui.utils.formatCompactUsd
import br.com.rmf.kmp.cryptoview.ui.utils.formatRelativeUpdate
import br.com.rmf.kmp.cryptoview.ui.utils.formatUsd
import br.com.rmf.kmp.cryptoview.ui.viewmodel.MarketViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val floatingNavigationPadding = LocalFloatingNavigationContentPadding.current
    var tabName by rememberSaveable { mutableStateOf(MarketTab.Coins.name) }
    var showSync by rememberSaveable { mutableStateOf(false) }
    val tab = MarketTab.valueOf(tabName)
    val coinGridState = rememberLazyGridState()
    val exchangeGridState = rememberLazyGridState()
    val gridState = if (tab == MarketTab.Coins) coinGridState else exchangeGridState

    LaunchedEffect(sync.status) {
        if (sync.status == SyncStatus.RUNNING) showSync = true
    }
    LaunchedEffect(tab, gridState) {
        snapshotFlow {
            val layout = gridState.layoutInfo
            val lastVisibleIndex = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            layout.totalItemsCount > 0 && lastVisibleIndex >= layout.totalItemsCount - PAGE_PREFETCH_DISTANCE
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd) {
                if (tab == MarketTab.Coins) marketViewModel.loadNextCoinsPage()
                else marketViewModel.loadNextExchangesPage()
            }
        }
    }
    DisposableEffect(Unit) { onDispose(marketViewModel::stopCoinDetails) }

    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(330.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize().widthIn(max = 1080.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 18.dp,
                end = 16.dp,
                bottom = 26.dp + floatingNavigationPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MarketHeader(
                    query = state.query,
                    searchVisible = state.searchVisible,
                    filtersVisible = state.filtersVisible,
                    selectedCoins = tab == MarketTab.Coins,
                    lastFetchedAt = state.coins.maxOfOrNull { it.quoteFetchedAt ?: 0L }?.takeIf { it > 0L },
                    syncing = sync.status == SyncStatus.RUNNING,
                    sortOrder = state.sortOrder,
                    variation = state.variationFilter,
                    exchangeFilters = state.availableExchangeFilters,
                    selectedExchangeId = state.selectedExchangeId,
                    onQueryChange = marketViewModel::setQuery,
                    onShowSearch = marketViewModel::showSearch,
                    onHideSearch = marketViewModel::hideSearch,
                    onToggleFilters = marketViewModel::toggleFilters,
                    onSortOrderChange = marketViewModel::setSortOrder,
                    onVariationChange = marketViewModel::setVariationFilter,
                    onExchangeChange = marketViewModel::setExchangeFilter,
                    onClearFilters = marketViewModel::clearFilterDraft,
                    onApplyFilters = marketViewModel::applyFilters,
                    onCoinsClick = { tabName = MarketTab.Coins.name },
                    onExchangesClick = {
                        if (state.filtersVisible) marketViewModel.toggleFilters()
                        tabName = MarketTab.Exchanges.name
                    },
                    onSync = {
                        marketViewModel.synchronize()
                        showSync = true
                    },
                )
            }

            if (tab == MarketTab.Coins) {
                val filtering = state.query.isNotBlank()
                    || state.appliedSortOrder != CoinSortOrder.MARKET_CAP
                    || state.appliedVariationFilter != CoinVariationFilter.ALL
                    || state.appliedExchangeId != null
                if (filtering) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "${state.coins.size} ${if (state.coins.size == 1) "resultado" else "resultados"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.coins.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyMarketMessage(
                        syncing = sync.status == SyncStatus.RUNNING || state.coinsLoading,
                        searching = filtering,
                    )
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
                        markets = state.expandedMarkets,
                        pollingIntervalSeconds = state.pollingIntervalSeconds,
                        detailsMessage = state.historyError?.let { "Histórico indisponível para esta chave." }
                            ?: state.marketsError?.let { "Corretoras indisponíveis para esta chave." },
                        onClick = { marketViewModel.expandCoin(coin.id) },
                        onViewAll = { onCoinMarketsClick(coin.id) },
                    )
                }
            } else {
                if (state.exchanges.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyMarketMessage(
                        syncing = sync.status == SyncStatus.RUNNING || state.exchangesLoading,
                        searching = state.query.isNotBlank(),
                    )
                }
                items(state.exchanges, key = { it.id }) { exchange ->
                    ExchangeCard(exchange) { onExchangeClick(exchange.id) }
                }
            }

            val loadingMore = if (tab == MarketTab.Coins) state.coinsLoading else state.exchangesLoading
            val loadedItems = if (tab == MarketTab.Coins) state.coins else state.exchanges
            if (loadingMore && loadedItems.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        Modifier.fillMaxWidth().height(46.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = CryptoOrange)
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
                    modifier = Modifier.fillMaxWidth(.92f).widthIn(max = 520.dp),
                    shape = RoundedCornerShape(18.dp),
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
private fun MarketHeader(
    query: String,
    searchVisible: Boolean,
    filtersVisible: Boolean,
    selectedCoins: Boolean,
    lastFetchedAt: Long?,
    syncing: Boolean,
    sortOrder: CoinSortOrder,
    variation: CoinVariationFilter,
    exchangeFilters: List<ExchangeSummary>,
    selectedExchangeId: Long?,
    onQueryChange: (String) -> Unit,
    onShowSearch: () -> Unit,
    onHideSearch: () -> Unit,
    onToggleFilters: () -> Unit,
    onSortOrderChange: (CoinSortOrder) -> Unit,
    onVariationChange: (CoinVariationFilter) -> Unit,
    onExchangeChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onApplyFilters: () -> Unit,
    onCoinsClick: () -> Unit,
    onExchangesClick: () -> Unit,
    onSync: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mercado", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.weight(1f))
            if (!searchVisible) {
                IconButton(onClick = onShowSearch) {
                    CryptoIcon(CryptoIcon.Search, "Buscar", Modifier.size(25.dp))
                }
            }
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (filtersVisible) CryptoOrangeSoft else Color.Transparent,
            ) {
                IconButton(onClick = onToggleFilters, enabled = selectedCoins) {
                    CryptoIcon(
                        CryptoIcon.Filter,
                        "Filtrar moedas",
                        Modifier.size(25.dp),
                        if (filtersVisible) CryptoOrange else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        AnimatedVisibility(searchVisible) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar moeda ou corretora") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { CryptoIcon(CryptoIcon.Search, "Buscar", Modifier.size(21.dp), CryptoOrange) },
                trailingIcon = {
                    IconButton(onClick = onHideSearch) {
                        CryptoIcon(CryptoIcon.Close, "Fechar busca", Modifier.size(20.dp))
                    }
                },
            )
        }
        MarketTabs(
            selectedCoins = selectedCoins,
            onCoinsClick = onCoinsClick,
            onExchangesClick = onExchangesClick,
        )
        AnimatedVisibility(filtersVisible && selectedCoins) {
            CoinFilters(
                sortOrder = sortOrder,
                variation = variation,
                exchanges = exchangeFilters,
                selectedExchangeId = selectedExchangeId,
                onSortOrderChange = onSortOrderChange,
                onVariationChange = onVariationChange,
                onExchangeChange = onExchangeChange,
                onClear = onClearFilters,
                onApply = onApplyFilters,
                onDismiss = onToggleFilters,
            )
        }
        if (!filtersVisible && query.isBlank()) {
            Row(
                modifier = Modifier.clickable(onClick = onSync).padding(horizontal = 2.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (syncing) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = CryptoOrange)
                } else {
                    CryptoIcon(CryptoIcon.Refresh, "Sincronizar mercado", Modifier.size(18.dp), CryptoOrange)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (syncing) "Sincronizando dados…" else formatRelativeUpdate(lastFetchedAt, currentTimeMillis()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CoinFilters(
    sortOrder: CoinSortOrder,
    variation: CoinVariationFilter,
    exchanges: List<ExchangeSummary>,
    selectedExchangeId: Long?,
    onSortOrderChange: (CoinSortOrder) -> Unit,
    onVariationChange: (CoinVariationFilter) -> Unit,
    onExchangeChange: (Long?) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), elevation = 3.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Filtros", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                CryptoIcon(
                    CryptoIcon.ChevronRight,
                    "Fechar filtros",
                    Modifier.size(20.dp).rotate(-90f).clickable(onClick = onDismiss),
                )
            }
            Text("Ordenar por", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChoice("Capitalização", sortOrder == CoinSortOrder.MARKET_CAP, { onSortOrderChange(CoinSortOrder.MARKET_CAP) }, Modifier.weight(1f))
                FilterChoice("Preço", sortOrder == CoinSortOrder.PRICE, { onSortOrderChange(CoinSortOrder.PRICE) }, Modifier.weight(1f))
            }
            Text("Variação em 24 horas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChoice("Todas", variation == CoinVariationFilter.ALL, { onVariationChange(CoinVariationFilter.ALL) }, Modifier.weight(1f))
                FilterChoice("Positivas", variation == CoinVariationFilter.POSITIVE, { onVariationChange(CoinVariationFilter.POSITIVE) }, Modifier.weight(1f))
                FilterChoice("Negativas", variation == CoinVariationFilter.NEGATIVE, { onVariationChange(CoinVariationFilter.NEGATIVE) }, Modifier.weight(1f))
            }
            Text("Corretora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChoice("Todas", selectedExchangeId == null, { onExchangeChange(null) }, Modifier.weight(1f))
                exchanges.take(2).forEach { exchange ->
                    FilterChoice(
                        exchange.name,
                        selectedExchangeId == exchange.id,
                        { onExchangeChange(exchange.id) },
                        Modifier.weight(1f),
                    )
                }
                repeat((2 - exchanges.take(2).size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
            if (exchanges.isEmpty()) {
                Text(
                    "As corretoras aparecem aqui após consultar os mercados de uma moeda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Limpar",
                    modifier = Modifier.clickable(onClick = onClear).padding(horizontal = 10.dp, vertical = 12.dp),
                    color = CryptoOrange,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                PrimaryActionButton("Aplicar", onApply, Modifier.widthIn(min = 126.dp))
            }
        }
    }
}

@Composable
private fun FilterChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = if (selected) CryptoOrange else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) CryptoOrange else CryptoBorder),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 9.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
        )
    }
}

@Composable
private fun EmptyMarketMessage(syncing: Boolean, searching: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (syncing) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
        Text(
            when {
                syncing -> "Preparando os dados do mercado…"
                searching -> "Nenhum resultado encontrado."
                else -> "Nenhum dado local. Inicie a sincronização."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CoinCard(
    coin: CoinSummary,
    expanded: Boolean,
    loading: Boolean,
    history: List<Float>,
    markets: List<CoinExchangeMarket>,
    pollingIntervalSeconds: Long,
    detailsMessage: String?,
    onClick: () -> Unit,
    onViewAll: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        onClick = onClick,
        borderColor = CryptoBorder.copy(alpha = .62f),
        borderWidth = .75.dp,
    ) {
        Column(
            Modifier.padding(
                horizontal = if (expanded) 16.dp else 12.dp,
                vertical = if (expanded) 15.dp else 10.dp,
            ),
        ) {
            CoinCardHeader(coin, expanded)
            AnimatedVisibility(expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(color = CryptoBorder, modifier = Modifier.fillMaxWidth().height(1.dp)) {}
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CryptoIcon(CryptoIcon.Refresh, "Atualização automática", Modifier.size(16.dp), CryptoOrange)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "${formatRelativeUpdate(coin.quoteFetchedAt, currentTimeMillis())} • atualiza a cada ${pollingIntervalSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("Últimas 24 horas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (loading && history.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(132.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                        }
                    }
                    if (history.isNotEmpty()) SparklineChart(history, Modifier.height(156.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric("Mín. 24h", formatUsd(history.minOrNull()?.toDouble()))
                        Metric("Máx. 24h", formatUsd(history.maxOrNull()?.toDouble()), Alignment.CenterHorizontally)
                        Metric("Volume 24h", formatCompactUsd(coin.volume24hUsd), Alignment.End)
                    }
                    if (markets.isNotEmpty()) {
                        Text("Disponível em", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val exchanges = markets.distinctBy { it.exchangeId }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            exchanges.take(4).forEach { market ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(58.dp)) {
                                    RemoteBrandLogo(
                                        market.exchangeLogoUrl,
                                        market.exchangeName.take(2).uppercase(),
                                        brandColor(market.exchangeId),
                                        Modifier.size(34.dp),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        market.exchangeName,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (exchanges.size > 4) {
                                Text("+${exchanges.size - 4}", color = CryptoOrange, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    detailsMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Ver todos os mercados",
                        modifier = Modifier.clickable(onClick = onViewAll).padding(vertical = 7.dp),
                        color = CryptoOrange,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoinCardHeader(coin: CoinSummary, expanded: Boolean) {
    Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        RemoteBrandLogo(coin.logoUrl, coin.symbol.take(2), brandColor(coin.id), Modifier.size(42.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                coin.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(coin.symbol, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatUsd(coin.priceUsd), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            coin.percentChange24h?.let { VariationPill(it) }
        }
        Spacer(Modifier.width(6.dp))
        CryptoIcon(
            CryptoIcon.ChevronRight,
            if (expanded) "Recolher detalhes" else "Expandir detalhes",
            Modifier.size(17.dp).rotate(if (expanded) 90f else 0f),
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExchangeCard(exchange: ExchangeSummary, onClick: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), onClick) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 98.dp).padding(horizontal = 16.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteBrandLogo(
                exchange.logoUrl,
                exchange.name.take(2).uppercase(),
                brandColor(exchange.id),
                Modifier.size(52.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(exchange.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    exchange.rank?.let { "Ranking #$it" } ?: "Ranking indisponível",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Volume 24h", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCompactUsd(exchange.spotVolumeUsd), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${exchange.numMarketPairs ?: 0} mercados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            CryptoIcon(CryptoIcon.ChevronRight, "Abrir ${exchange.name}", Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun brandColor(id: Long): Color = listOf(
    Color(0xFFF4511E), Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A),
)[(id % 4).toInt()]

private const val PAGE_PREFETCH_DISTANCE = 6
