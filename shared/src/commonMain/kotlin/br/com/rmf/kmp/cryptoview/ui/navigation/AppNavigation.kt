package br.com.rmf.kmp.cryptoview.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.domain.sync.CryptoSyncManager
import br.com.rmf.kmp.cryptoview.ui.screens.CoinMarketsScreen
import br.com.rmf.kmp.cryptoview.ui.screens.CoinInformationScreen
import br.com.rmf.kmp.cryptoview.ui.screens.ExchangeDetailScreen
import br.com.rmf.kmp.cryptoview.ui.screens.MarketScreen
import br.com.rmf.kmp.cryptoview.ui.screens.SettingsScreen
import br.com.rmf.kmp.cryptoview.ui.components.LocalFloatingNavigationContentPadding
import br.com.rmf.kmp.cryptoview.ui.components.SyncProgressContent
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrangeSoft
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoBorder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.koin.mp.KoinPlatformTools

sealed interface AppRoute : NavKey
data object MarketRoute : AppRoute
data object SettingsRoute : AppRoute
data class ExchangeDetailRoute(val exchangeId: Long) : AppRoute
data class CoinMarketsRoute(val coinId: Long) : AppRoute
data class CoinInformationRoute(val coinId: Long, val coinPaprikaId: String) : AppRoute

private class AppNavigationState {
    private val marketStack = mutableStateListOf<AppRoute>(MarketRoute)
    private val settingsStack = mutableStateListOf<AppRoute>(SettingsRoute)

    var selectedTopLevel: AppRoute by mutableStateOf(MarketRoute)
        private set

    val currentStack: SnapshotStateList<AppRoute>
        get() = if (selectedTopLevel == SettingsRoute) settingsStack else marketStack

    fun selectTopLevel(route: AppRoute) {
        if (route == selectedTopLevel) {
            while (currentStack.size > 1) currentStack.removeLastOrNull()
        } else {
            selectedTopLevel = route
        }
    }

    fun navigate(route: AppRoute) = currentStack.add(route)

    fun goBack() {
        if (currentStack.size > 1) currentStack.removeLastOrNull()
        else selectedTopLevel = MarketRoute
    }
}

@Composable
fun AppNavigation(
    keyInfo: CoinMarketCapKeyInfo?,
    validationMessage: String?,
    onRevalidateKey: () -> Unit,
    onReplaceKey: () -> Unit,
    onRemoveKey: () -> Unit,
) {
    val navigation = remember { AppNavigationState() }
    val syncManager = remember {
        KoinPlatformTools.defaultContext().get().get<CryptoSyncManager>()
    }
    val sync by syncManager.state.collectAsStateWithLifecycle()
    var syncDialogVisible by remember { mutableStateOf(false) }
    val syncRunning = sync.status == SyncStatus.RUNNING

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxWidth < 600.dp
            if (compact) {
                Box(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalFloatingNavigationContentPadding provides 82.dp) {
                        AppNavigationContent(
                            navigation = navigation,
                            keyInfo = keyInfo,
                            validationMessage = validationMessage,
                            onRevalidateKey = onRevalidateKey,
                            onReplaceKey = onReplaceKey,
                            onRemoveKey = onRemoveKey,
                            syncRunning = syncRunning,
                            syncDialogVisible = syncDialogVisible,
                            onShowSync = { syncDialogVisible = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    FloatingBottomNavigation(
                        selectedRoute = navigation.selectedTopLevel,
                        onMarketClick = { navigation.selectTopLevel(MarketRoute) },
                        onSettingsClick = { navigation.selectTopLevel(SettingsRoute) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                return@BoxWithConstraints
            }

            val navigationItemColors = NavigationSuiteDefaults.itemColors(
                navigationBarItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CryptoOrange,
                    selectedTextColor = CryptoOrange,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                navigationRailItemColors = NavigationRailItemDefaults.colors(
                    selectedIconColor = CryptoOrange,
                    selectedTextColor = CryptoOrange,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    item(
                        selected = navigation.selectedTopLevel == MarketRoute,
                        onClick = { navigation.selectTopLevel(MarketRoute) },
                        icon = { NavigationGlyph(false, navigation.selectedTopLevel == MarketRoute) },
                        label = { Text("Mercado", style = MaterialTheme.typography.labelLarge) },
                        colors = navigationItemColors,
                    )
                    item(
                        selected = navigation.selectedTopLevel == SettingsRoute,
                        onClick = { navigation.selectTopLevel(SettingsRoute) },
                        icon = { NavigationGlyph(true, navigation.selectedTopLevel == SettingsRoute) },
                        label = { Text("Ajustes", style = MaterialTheme.typography.labelLarge) },
                        colors = navigationItemColors,
                    )
                },
                layoutType = NavigationSuiteType.NavigationRail,
                navigationSuiteColors = NavigationSuiteDefaults.colors(
                    navigationBarContainerColor = MaterialTheme.colorScheme.surface,
                    navigationBarContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationRailContainerColor = MaterialTheme.colorScheme.surface,
                    navigationRailContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                AppNavigationContent(
                    navigation = navigation,
                    keyInfo = keyInfo,
                    validationMessage = validationMessage,
                    onRevalidateKey = onRevalidateKey,
                    onReplaceKey = onReplaceKey,
                    onRemoveKey = onRemoveKey,
                    syncRunning = syncRunning,
                    syncDialogVisible = syncDialogVisible,
                    onShowSync = { syncDialogVisible = true },
                )
            }
        }

        if (syncDialogVisible) {
            Dialog(
                onDismissRequest = { syncDialogVisible = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(.92f).widthIn(max = 520.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SyncProgressContent(
                        progress = sync,
                        onBackground = { syncDialogVisible = false },
                        onCancel = syncManager::cancel,
                        onResume = {
                            syncManager.resume()
                            syncDialogVisible = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavigationContent(
    navigation: AppNavigationState,
    keyInfo: CoinMarketCapKeyInfo?,
    validationMessage: String?,
    onRevalidateKey: () -> Unit,
    onReplaceKey: () -> Unit,
    onRemoveKey: () -> Unit,
    syncRunning: Boolean,
    syncDialogVisible: Boolean,
    onShowSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        modifier = modifier,
        backStack = navigation.currentStack,
        onBack = navigation::goBack,
        entryProvider = entryProvider {
            entry<MarketRoute> {
                MarketScreen(
                    onExchangeClick = { navigation.navigate(ExchangeDetailRoute(it)) },
                    onCoinMarketsClick = { navigation.navigate(CoinMarketsRoute(it)) },
                    onCoinInformationClick = { coinId, paprikaId ->
                        navigation.navigate(CoinInformationRoute(coinId, paprikaId))
                    },
                    syncDialogVisible = syncDialogVisible,
                    onShowSync = onShowSync,
                )
            }
            entry<SettingsRoute> {
                SettingsScreen(
                    keyInfo = keyInfo,
                    validationMessage = validationMessage,
                    onRevalidateKey = onRevalidateKey,
                    onReplaceKey = onReplaceKey,
                    onRemoveKey = onRemoveKey,
                    syncDialogVisible = syncDialogVisible,
                    onShowSync = onShowSync,
                )
            }
            entry<ExchangeDetailRoute> { route ->
                ExchangeDetailScreen(
                    exchangeId = route.exchangeId,
                    onBack = navigation::goBack,
                    showSyncIndicator = syncRunning && !syncDialogVisible,
                    onShowSync = onShowSync,
                )
            }
            entry<CoinMarketsRoute> { route ->
                CoinMarketsScreen(
                    coinId = route.coinId,
                    onBack = navigation::goBack,
                    onExchangeClick = { navigation.navigate(ExchangeDetailRoute(it)) },
                    showSyncIndicator = syncRunning && !syncDialogVisible,
                    onShowSync = onShowSync,
                )
            }
            entry<CoinInformationRoute> { route ->
                CoinInformationScreen(
                    coinId = route.coinId,
                    coinPaprikaId = route.coinPaprikaId,
                    onBack = navigation::goBack,
                    showSyncIndicator = syncRunning && !syncDialogVisible,
                    onShowSync = onShowSync,
                )
            }
        },
    )
}

@Composable
private fun FloatingBottomNavigation(
    selectedRoute: AppRoute,
    onMarketClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 310.dp).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .84f),
            shadowElevation = 5.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, CryptoBorder.copy(alpha = .78f)),
        ) {
            Row(
                modifier = Modifier.padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FloatingNavigationItem(
                    label = "Mercado",
                    settings = false,
                    selected = selectedRoute == MarketRoute,
                    onClick = onMarketClick,
                    modifier = Modifier.weight(1f),
                )
                FloatingNavigationItem(
                    label = "Ajustes",
                    settings = true,
                    selected = selectedRoute == SettingsRoute,
                    onClick = onSettingsClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FloatingNavigationItem(
    label: String,
    settings: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(27.dp)
    Column(
        modifier = modifier
            .height(60.dp)
            .clip(shape)
            .background(if (selected) CryptoOrangeSoft.copy(alpha = .70f) else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NavigationGlyph(settings = settings, selected = selected, floating = true)
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) CryptoOrange else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun NavigationGlyph(settings: Boolean, selected: Boolean, floating: Boolean = false) {
    val color = if (selected) CryptoOrange else MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (!floating) {
            Spacer(
                Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .background(if (selected) CryptoOrange else Color.Transparent),
            )
            Spacer(Modifier.height(5.dp))
        }
        Canvas(Modifier.size(if (floating) 23.dp else 22.dp)) {
            if (!settings) {
                listOf(.45f, .78f, .60f).forEachIndexed { index, height ->
                    val width = size.width * .18f
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(size.width * .10f + index * size.width * .30f, size.height * (1f - height)),
                        size = androidx.compose.ui.geometry.Size(width, size.height * height),
                    )
                }
            } else {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(color, size.minDimension * .31f, center, style = Stroke(2.2.dp.toPx()))
                drawCircle(color, size.minDimension * .10f, center, style = Stroke(2.2.dp.toPx()))
                repeat(8) { index ->
                    val angle = index * PI / 4.0
                    drawLine(
                        color,
                        Offset(center.x + cos(angle).toFloat() * size.minDimension * .36f, center.y + sin(angle).toFloat() * size.minDimension * .36f),
                        Offset(center.x + cos(angle).toFloat() * size.minDimension * .47f, center.y + sin(angle).toFloat() * size.minDimension * .47f),
                        2.2.dp.toPx(),
                        StrokeCap.Round,
                    )
                }
            }
        }
        if (floating) {
            Spacer(Modifier.height(3.dp))
            Spacer(
                Modifier
                    .width(26.dp)
                    .height(3.dp)
                    .background(if (selected) CryptoOrange else Color.Transparent, RoundedCornerShape(2.dp)),
            )
        }
    }
}
