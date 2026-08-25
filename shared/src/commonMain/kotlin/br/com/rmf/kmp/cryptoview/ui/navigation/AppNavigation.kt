package br.com.rmf.kmp.cryptoview.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import br.com.rmf.kmp.cryptoview.domain.model.CoinMarketCapKeyInfo
import br.com.rmf.kmp.cryptoview.ui.screens.CoinMarketsScreen
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
data class ExchangeDetailRoute(val exchangeId: Long) : AppRoute
data class CoinMarketsRoute(val coinId: Long) : AppRoute

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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layoutType = if (maxWidth < 600.dp) NavigationSuiteType.NavigationBar
        else NavigationSuiteType.NavigationRail

        NavigationSuiteScaffold(
            navigationSuiteItems = {
                item(
                    selected = navigation.selectedTopLevel == MarketRoute,
                    onClick = { navigation.selectTopLevel(MarketRoute) },
                    icon = { NavigationGlyph(false, navigation.selectedTopLevel == MarketRoute) },
                    label = { Text("Mercado", style = MaterialTheme.typography.labelMedium) },
                )
                item(
                    selected = navigation.selectedTopLevel == SettingsRoute,
                    onClick = { navigation.selectTopLevel(SettingsRoute) },
                    icon = { NavigationGlyph(true, navigation.selectedTopLevel == SettingsRoute) },
                    label = { Text("Ajustes", style = MaterialTheme.typography.labelMedium) },
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
                            keyInfo = keyInfo,
                            validationMessage = validationMessage,
                            onRevalidateKey = onRevalidateKey,
                            onReplaceKey = onReplaceKey,
                            onRemoveKey = onRemoveKey,
                        )
                    }
                    entry<ExchangeDetailRoute> { route ->
                        ExchangeDetailScreen(route.exchangeId, navigation::goBack)
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
private fun NavigationGlyph(settings: Boolean, selected: Boolean) {
    val color = if (selected) CryptoOrange else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(21.dp)) {
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
}
