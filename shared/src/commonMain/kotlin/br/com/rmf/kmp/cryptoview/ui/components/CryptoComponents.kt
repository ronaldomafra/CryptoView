package br.com.rmf.kmp.cryptoview.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import br.com.rmf.kmp.cryptoview.domain.model.SyncProgress
import br.com.rmf.kmp.cryptoview.domain.model.SyncPhase
import br.com.rmf.kmp.cryptoview.domain.model.SyncStatus
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoBorder
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoNegative
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoNegativeSoft
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrangeSoft
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoPositive
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoPositiveSoft
import br.com.rmf.kmp.cryptoview.ui.utils.formatPercentage
import coil3.compose.AsyncImage

enum class CryptoIcon {
    Search,
    Filter,
    Close,
    Refresh,
    ChevronRight,
    Lock,
    Eye,
    EyeOff,
}

@Composable
fun CryptoIcon(
    icon: CryptoIcon,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val stroke = 2.dp.toPx()
        when (icon) {
            CryptoIcon.Search -> {
                drawCircle(color, size.minDimension * .28f, Offset(size.width * .43f, size.height * .43f), style = Stroke(stroke))
                drawLine(color, Offset(size.width * .63f, size.height * .63f), Offset(size.width * .86f, size.height * .86f), stroke, StrokeCap.Round)
            }
            CryptoIcon.Filter -> {
                val ys = listOf(.25f, .5f, .75f)
                val knobs = listOf(.36f, .68f, .48f)
                ys.forEachIndexed { index, y ->
                    drawLine(color, Offset(size.width * .12f, size.height * y), Offset(size.width * .88f, size.height * y), stroke, StrokeCap.Round)
                    drawCircle(color, size.minDimension * .075f, Offset(size.width * knobs[index], size.height * y))
                }
            }
            CryptoIcon.Close -> {
                drawLine(color, Offset(size.width * .22f, size.height * .22f), Offset(size.width * .78f, size.height * .78f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .78f, size.height * .22f), Offset(size.width * .22f, size.height * .78f), stroke, StrokeCap.Round)
            }
            CryptoIcon.Refresh -> {
                drawArc(color, -55f, 245f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(color, 125f, 125f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                drawLine(color, Offset(size.width * .72f, size.height * .12f), Offset(size.width * .86f, size.height * .18f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .86f, size.height * .18f), Offset(size.width * .83f, size.height * .34f), stroke, StrokeCap.Round)
            }
            CryptoIcon.ChevronRight -> {
                drawLine(color, Offset(size.width * .36f, size.height * .22f), Offset(size.width * .66f, size.height * .5f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .66f, size.height * .5f), Offset(size.width * .36f, size.height * .78f), stroke, StrokeCap.Round)
            }
            CryptoIcon.Lock -> {
                drawRoundRect(color, Offset(size.width * .2f, size.height * .43f), Size(size.width * .6f, size.height * .45f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * .08f), style = Stroke(stroke))
                drawArc(color, 180f, 180f, false, topLeft = Offset(size.width * .31f, size.height * .12f), size = Size(size.width * .38f, size.height * .5f), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            CryptoIcon.Eye, CryptoIcon.EyeOff -> {
                val path = Path().apply {
                    moveTo(size.width * .08f, size.height * .5f)
                    quadraticTo(size.width * .5f, size.height * .08f, size.width * .92f, size.height * .5f)
                    quadraticTo(size.width * .5f, size.height * .92f, size.width * .08f, size.height * .5f)
                    close()
                }
                drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
                drawCircle(color, size.minDimension * .12f, center)
                if (icon == CryptoIcon.EyeOff) {
                    drawLine(surfaceColor, Offset(size.width * .12f, size.height * .12f), Offset(size.width * .88f, size.height * .88f), stroke * 2.5f, StrokeCap.Round)
                    drawLine(color, Offset(size.width * .12f, size.height * .12f), Offset(size.width * .88f, size.height * .88f), stroke, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
fun CryptoViewLogo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(width = 70.dp, height = 42.dp)) {
                val points = listOf(
                    Offset(size.width * .08f, size.height * .78f),
                    Offset(size.width * .42f, size.height * .36f),
                    Offset(size.width * .68f, size.height * .48f),
                    Offset(size.width * .94f, size.height * .08f),
                )
                for (index in 0 until points.lastIndex) {
                    drawLine(CryptoOrange, points[index], points[index + 1], 5.dp.toPx(), StrokeCap.Round)
                }
                points.forEach { drawCircle(CryptoOrange, 6.5.dp.toPx(), it) }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(4.dp, CryptoOrange, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("₿", color = CryptoOrange, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }
        Text(
            text = "CryptoView",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CryptoOrange,
            contentColor = Color.White,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
fun MarketTabs(
    selectedCoins: Boolean,
    onCoinsClick: () -> Unit,
    onExchangesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CryptoOrange, RoundedCornerShape(13.dp))
            .padding(3.dp),
    ) {
        MarketTab(
            text = "Moedas",
            selected = selectedCoins,
            onClick = onCoinsClick,
            modifier = Modifier.weight(1f),
        )
        MarketTab(
            text = "Corretoras",
            selected = !selectedCoins,
            onClick = onExchangesClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MarketTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 12.dp),
            color = if (selected) CryptoOrange else Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun RoundBrandLogo(
    glyph: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    foregroundColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = foregroundColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RemoteBrandLogo(
    imageUrl: String?,
    glyph: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        RoundBrandLogo(glyph, backgroundColor, Modifier.fillMaxSize())
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}

@Composable
fun VariationPill(value: Double) {
    val positive = value >= 0
    Surface(
        color = if (positive) CryptoPositiveSoft else CryptoNegativeSoft,
        contentColor = if (positive) CryptoPositive else CryptoNegative,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = formatPercentage(value),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 0.dp,
    borderColor: Color = CryptoBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
    ) {
        content()
    }
}

@Composable
fun SparklineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val trend = if (values.lastOrNull().let { last -> last != null && last >= (values.firstOrNull() ?: last) }) "alta" else "baixa"
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .semantics { contentDescription = "Gráfico das últimas 24 horas com tendência de $trend" },
    ) {
        if (values.size < 2) return@Canvas
        val horizontalPadding = 2.dp.toPx()
        val verticalPadding = 8.dp.toPx()
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - verticalPadding * 2
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val points = values.mapIndexed { index, value ->
            Offset(
                x = horizontalPadding + chartWidth * index / values.lastIndex,
                y = verticalPadding + chartHeight * (1f - ((value - min) / range)),
            )
        }
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val fillPath = Path().apply {
            moveTo(points.first().x, size.height)
            lineTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(listOf(CryptoOrange.copy(alpha = .24f), Color.Transparent)),
        )
        drawPath(linePath, CryptoOrange, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun SyncProgressContent(
    progress: SyncProgress,
    onBackground: () -> Unit,
    onCancel: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = (progress.percentage ?: if (progress.status == SyncStatus.COMPLETED) 1f else 0f).coerceIn(0f, 1f)
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            syncTitle(progress.status),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        SyncSteps(progress)
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                progress.message ?: phaseLabel(progress.phase),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "${(fraction * 100).toInt()}%",
                color = CryptoOrange,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(10.dp))
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            drawRoundRect(CryptoOrangeSoft, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            drawRoundRect(CryptoOrange, size = Size(size.width * fraction, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            progress.plannedItems?.let { "${progress.persistedItems} de $it ${phaseItemName(progress.phase)}" }
                ?: "${progress.persistedItems} ${phaseItemName(progress.phase)} salvos",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(18.dp))
        Text("Salvando dados no dispositivo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text("Você pode continuar usando o app.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        PrimaryActionButton(
            if (progress.status == SyncStatus.RUNNING) "Executar em segundo plano" else "Fechar",
            onBackground,
            Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (progress.status == SyncStatus.RUNNING) {
            Text("Cancelar", Modifier.clickable(onClick = onCancel).padding(12.dp), color = CryptoOrange, fontWeight = FontWeight.SemiBold)
        } else if (progress.status == SyncStatus.PAUSED || progress.status == SyncStatus.FAILED) {
            Text("Retomar", Modifier.clickable(onClick = onResume).padding(12.dp), color = CryptoOrange, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SyncRunningAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp).semantics {
                contentDescription = "Sincronização em andamento. Toque para acompanhar."
            },
            color = CryptoOrange,
            strokeWidth = 2.5.dp,
        )
    }
}

@Composable
private fun SyncSteps(progress: SyncProgress) {
    val labels = listOf("Acesso", "Preparar", "Corretoras", "Moedas", "Concluir")
    val activeStep = syncStepIndex(progress.phase)
    val finished = progress.status == SyncStatus.COMPLETED || progress.status == SyncStatus.PARTIAL
    val lineFraction = if (finished) 1f else activeStep.toFloat() / (labels.lastIndex.coerceAtLeast(1))
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val stepWidth = size.width / labels.size
                val startX = stepWidth / 2f
                val endX = size.width - startX
                val progressEnd = startX + (endX - startX) * lineFraction
                drawLine(CryptoBorder, Offset(startX, size.height / 2f), Offset(endX, size.height / 2f), size.height, StrokeCap.Round)
                drawLine(CryptoOrange, Offset(startX, size.height / 2f), Offset(progressEnd, size.height / 2f), size.height, StrokeCap.Round)
            }
            Row(Modifier.fillMaxWidth()) {
                labels.indices.forEach { index ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(16.dp)) {
                            val reached = finished || index <= activeStep
                            drawCircle(if (reached) CryptoOrange else CryptoOrangeSoft)
                            if (index == activeStep && !finished) {
                                drawCircle(surfaceColor, radius = size.minDimension * .20f)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = if (finished || index <= activeStep) CryptoOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == activeStep) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun syncStepIndex(phase: SyncPhase): Int = when (phase) {
    SyncPhase.VALIDATING_CREDENTIAL -> 0
    SyncPhase.PREPARING, SyncPhase.RESTORING_CHECKPOINT -> 1
    SyncPhase.EXCHANGES, SyncPhase.EXCHANGE_METADATA -> 2
    SyncPhase.COINS, SyncPhase.COIN_METADATA -> 3
    SyncPhase.FINALIZING, SyncPhase.COMPLETED -> 4
}

private fun phaseLabel(phase: SyncPhase): String = when (phase) {
    SyncPhase.VALIDATING_CREDENTIAL -> "Validando acesso"
    SyncPhase.PREPARING -> "Preparando sincronização"
    SyncPhase.RESTORING_CHECKPOINT -> "Restaurando progresso"
    SyncPhase.EXCHANGES -> "Baixando corretoras"
    SyncPhase.EXCHANGE_METADATA -> "Atualizando corretoras"
    SyncPhase.COINS -> "Baixando moedas"
    SyncPhase.COIN_METADATA -> "Atualizando moedas"
    SyncPhase.FINALIZING -> "Finalizando"
    SyncPhase.COMPLETED -> "Concluído"
}

private fun syncTitle(status: SyncStatus): String = when (status) {
    SyncStatus.COMPLETED -> "Mercado atualizado"
    SyncStatus.PARTIAL -> "Mercado atualizado parcialmente"
    SyncStatus.PAUSED -> "Sincronização pausada"
    SyncStatus.FAILED -> "Não foi possível sincronizar"
    else -> "Sincronizando mercado"
}

private fun phaseItemName(phase: SyncPhase): String = when (phase) {
    SyncPhase.COINS, SyncPhase.COIN_METADATA -> "moedas"
    SyncPhase.EXCHANGES, SyncPhase.EXCHANGE_METADATA -> "corretoras"
    else -> "itens"
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = CryptoBorder, modifier = Modifier.padding(vertical = 4.dp))
}
