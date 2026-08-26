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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rmf.kmp.cryptoview.domain.model.CoinInformationFailure
import br.com.rmf.kmp.cryptoview.ui.components.CryptoIcon
import br.com.rmf.kmp.cryptoview.ui.components.InfoRow
import br.com.rmf.kmp.cryptoview.ui.components.LocalFloatingNavigationContentPadding
import br.com.rmf.kmp.cryptoview.ui.components.OutlinedCard
import br.com.rmf.kmp.cryptoview.ui.components.RemoteBrandLogo
import br.com.rmf.kmp.cryptoview.ui.components.SyncRunningAction
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.viewmodel.CoinInformationViewModel
import org.koin.mp.KoinPlatformTools

@Composable
fun CoinInformationScreen(
    coinId: Long,
    coinPaprikaId: String,
    onBack: () -> Unit,
    showSyncIndicator: Boolean,
    onShowSync: () -> Unit,
) {
    val informationViewModel = viewModel<CoinInformationViewModel> {
        KoinPlatformTools.defaultContext().get().get<CoinInformationViewModel>()
    }
    val state by informationViewModel.uiState.collectAsStateWithLifecycle()
    val floatingNavigationPadding = LocalFloatingNavigationContentPadding.current
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(coinId, coinPaprikaId) {
        informationViewModel.load(coinId, coinPaprikaId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .widthIn(max = 900.dp),
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
                    CryptoIcon(
                        CryptoIcon.ChevronRight,
                        "Voltar",
                        Modifier.size(23.dp).rotate(180f),
                        CryptoOrange,
                    )
                }
                Column {
                    Text(
                        "Informações da moeda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        state.coin?.name ?: "Moeda",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (showSyncIndicator) SyncRunningAction(onClick = onShowSync)
            }
        }

        if (state.loading && state.information == null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = CryptoOrange)
                    Spacer(Modifier.width(10.dp))
                    Text("Carregando informações…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        state.information?.let { information ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RemoteBrandLogo(
                                information.logoUrl ?: state.coin?.logoUrl,
                                information.symbol.take(2).uppercase(),
                                Color(0xFFF4511E),
                                Modifier.size(54.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(information.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "${information.symbol} · ${information.type.displayCoinType()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        information.message?.let {
                            Text(it, color = CryptoOrange, style = MaterialTheme.typography.bodyMedium)
                        }
                        information.description?.let { Text(it) }
                        InfoRow("Ranking", information.rank?.let { "#$it" } ?: "—")
                        InfoRow("Status", if (information.isActive) "Ativa" else "Inativa")
                    }
                }
            }

            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Projeto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        InfoRow("Lançamento", information.startedAt?.take(10) ?: "—")
                        InfoRow("Desenvolvimento", information.developmentStatus ?: "—")
                        InfoRow("Código aberto", information.openSource.displayBoolean())
                        InfoRow("Hardware wallet", information.hardwareWallet.displayBoolean())
                    }
                }
            }

            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tecnologia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        InfoRow("Consenso", information.proofType ?: "—")
                        InfoRow("Algoritmo", information.hashAlgorithm ?: "—")
                        InfoRow("Organização", information.orgStructure ?: "—")
                    }
                }
            }

            val links = listOfNotNull(
                information.websiteUrl?.let { "Website" to it },
                information.explorerUrl?.let { "Explorador" to it },
                information.sourceCodeUrl?.let { "Código-fonte" to it },
                information.whitepaperUrl?.let { "Whitepaper" to it },
            )
            if (links.isNotEmpty()) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            links.forEach { (label, url) ->
                                ExternalLinkRow(label = label, onClick = { uriHandler.openUri(url) })
                            }
                        }
                    }
                }
            }
        }

        state.failure?.let { failure ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            failure.displayMessage(state.information != null),
                            color = if (state.information == null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (failure is CoinInformationFailure.Request) {
                            Text(
                                "Tentar novamente",
                                modifier = Modifier.clickable(onClick = informationViewModel::retry).padding(vertical = 6.dp),
                                color = CryptoOrange,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        if (state.refreshing) {
            item {
                Text("Atualizando dados…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExternalLinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Text("Abrir", color = CryptoOrange, fontWeight = FontWeight.SemiBold)
    }
}

private fun String?.displayCoinType(): String = when (this?.lowercase()) {
    "coin" -> "Moeda"
    "token" -> "Token"
    else -> this ?: "Tipo indisponível"
}

private fun Boolean?.displayBoolean(): String = when (this) {
    true -> "Sim"
    false -> "Não"
    null -> "—"
}

private fun CoinInformationFailure.displayMessage(hasCache: Boolean): String = when (this) {
    CoinInformationFailure.UnresolvedIdentity ->
        "Não foi possível confirmar esta moeda na CoinPaprika com segurança."
    is CoinInformationFailure.Request -> if (hasCache) {
        "Não foi possível atualizar. Exibindo as últimas informações salvas."
    } else {
        "Não foi possível carregar as informações da moeda."
    }
}
