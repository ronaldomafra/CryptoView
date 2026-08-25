package br.com.rmf.kmp.cryptoview.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import br.com.rmf.kmp.cryptoview.ui.screens.CoinMarketsScreen
import br.com.rmf.kmp.cryptoview.ui.screens.CoinMarketCapTestScreen
import br.com.rmf.kmp.cryptoview.ui.screens.ExchangeDetailScreen
import br.com.rmf.kmp.cryptoview.ui.screens.MarketScreen
import br.com.rmf.kmp.cryptoview.ui.screens.SettingsScreen
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

sealed interface AppRoute : NavKey
data object MarketRoute : AppRoute
data object SettingsRoute : AppRoute
data object TestsRoute : AppRoute
data class ExchangeDetailRoute(val exchangeId: Int) : AppRoute
data class CoinMarketsRoute(val coinId: Int) : AppRoute

private class AppNavigationState {
    private val marketStack = mutableStateListOf<AppRoute>(MarketRoute)
    private val settingsStack = mutableStateListOf<AppRoute>(SettingsRoute)
    private val testsStack = mutableStateListOf<AppRoute>(TestsRoute)

    var selectedTopLevel: AppRoute by mutableStateOf(MarketRoute)
        private set

    val currentStack: SnapshotStateList<AppRoute>
        get() = when (selectedTopLevel) {
            MarketRoute -> marketStack
            SettingsRoute -> settingsStack
            TestsRoute -> testsStack
            else -> marketStack
        }

    fun selectTopLevel(route: AppRoute) {
        if (route == selectedTopLevel) {
            while (currentStack.size > 1) currentStack.removeLastOrNull()
        } else {
            selectedTopLevel = route
        }
    }

    fun navigate(route: AppRoute) {
        currentStack.add(route)
    }

    fun goBack() {
        if (currentStack.size > 1) {
            currentStack.removeLastOrNull()
        } else if (selectedTopLevel != MarketRoute) {
            selectedTopLevel = MarketRoute
        }
    }
}

@Composable
fun AppNavigation(onApiKeyReset: () -> Unit) {
    val navigation = remember { AppNavigationState() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layoutType = if (maxWidth < 600.dp) {
            NavigationSuiteType.NavigationBar
        } else {
            NavigationSuiteType.NavigationRail
        }

        NavigationSuiteScaffold(
            navigationSuiteItems = {
                item(
                    selected = navigation.selectedTopLevel == MarketRoute,
                    onClick = { navigation.selectTopLevel(MarketRoute) },
                    icon = {
                        NavigationGlyph(
                            type = NavigationGlyphType.Market,
                            selected = navigation.selectedTopLevel == MarketRoute,
                        )
                    },
                    label = { androidx.compose.material3.Text("Mercado", style = MaterialTheme.typography.labelMedium) },
                )
                item(
                    selected = navigation.selectedTopLevel == SettingsRoute,
                    onClick = { navigation.selectTopLevel(SettingsRoute) },
                    icon = {
                        NavigationGlyph(
                            type = NavigationGlyphType.Settings,
                            selected = navigation.selectedTopLevel == SettingsRoute,
                        )
                    },
                    label = { androidx.compose.material3.Text("Ajustes", style = MaterialTheme.typography.labelMedium) },
                )
                item(
                    selected = navigation.selectedTopLevel == TestsRoute,
                    onClick = { navigation.selectTopLevel(TestsRoute) },
                    icon = {
                        NavigationGlyph(
                            type = NavigationGlyphType.Tests,
                            selected = navigation.selectedTopLevel == TestsRoute,
                        )
                    },
                    label = { androidx.compose.material3.Text("Testes", style = MaterialTheme.typography.labelMedium) },
                )
            },
            layoutType = layoutType,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            NavDisplay(
                backStack = navigation.currentStack,
                onBack = navigation::goBack,
                entryProvider = entryProvider {
                    entry<MarketRoute> {
                        MarketScreen(
                            onExchangeClick = { navigation.navigate(ExchangeDetailRoute(it)) },
                            onCoinMarketsClick = { navigation.navigate(CoinMarketsRoute(it)) },
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(
                            onReplaceKey = onApiKeyReset,
                            onRemoveKey = onApiKeyReset,
                        )
                    }
                    entry<TestsRoute> {
                        CoinMarketCapTestScreen()
                    }
                    entry<ExchangeDetailRoute> { route ->
                        ExchangeDetailScreen(
                            exchangeId = route.exchangeId,
                            onBack = navigation::goBack,
                        )
                    }
                    entry<CoinMarketsRoute> { route ->
                        CoinMarketsScreen(
                            coinId = route.coinId,
                            onBack = navigation::goBack,
                            onExchangeClick = { navigation.navigate(ExchangeDetailRoute(it)) },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun NavigationGlyph(type: NavigationGlyphType, selected: Boolean) {
    val color = if (selected) CryptoOrange else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(22.dp)) {
        when (type) {
            NavigationGlyphType.Market -> {
                val barWidth = size.width * .18f
                val gap = size.width * .12f
                val heights = listOf(.45f, .78f, .60f)
                heights.forEachIndexed { index, height ->
                    val left = size.width * .10f + index * (barWidth + gap)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, size.height * (1f - height)),
                        size = androidx.compose.ui.geometry.Size(barWidth, size.height * height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3),
                    )
                }
            }

            NavigationGlyphType.Settings -> {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = color,
                    radius = size.minDimension * .31f,
                    center = center,
                    style = Stroke(2.5.dp.toPx()),
                )
                drawCircle(
                    color = color,
                    radius = size.minDimension * .10f,
                    center = center,
                    style = Stroke(2.5.dp.toPx()),
                )
                repeat(8) { index ->
                    val angle = index * PI / 4.0
                    val inner = size.minDimension * .36f
                    val outer = size.minDimension * .47f
                    drawLine(
                        color = color,
                        start = Offset(
                            center.x + cos(angle).toFloat() * inner,
                            center.y + sin(angle).toFloat() * inner,
                        ),
                        end = Offset(
                            center.x + cos(angle).toFloat() * outer,
                            center.y + sin(angle).toFloat() * outer,
                        ),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            NavigationGlyphType.Tests -> {
                drawRoundRect(
                    color = color,
                    style = Stroke(2.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * .24f, size.height * .35f),
                    end = Offset(size.width * .43f, size.height * .50f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * .43f, size.height * .50f),
                    end = Offset(size.width * .24f, size.height * .65f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * .52f, size.height * .66f),
                    end = Offset(size.width * .76f, size.height * .66f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private enum class NavigationGlyphType {
    Market,
    Settings,
    Tests,
}
