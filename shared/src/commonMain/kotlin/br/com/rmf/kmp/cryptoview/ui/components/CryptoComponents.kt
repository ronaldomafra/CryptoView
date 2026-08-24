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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rmf.kmp.cryptoview.ui.model.MockExchangeBadge
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoBorder
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoNegative
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoNegativeSoft
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrangeSoft
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoPositive
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoPositiveSoft

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
        modifier = modifier.height(50.dp),
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
            .background(CryptoOrange, RoundedCornerShape(12.dp))
            .padding(4.dp),
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
        shape = RoundedCornerShape(9.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 10.dp),
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
fun ExchangeBadges(
    exchanges: List<MockExchangeBadge>,
    additional: Int,
    modifier: Modifier = Modifier,
    showNames: Boolean = false,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        exchanges.take(3).forEachIndexed { index, exchange ->
            Row(
                modifier = if (index == 0) Modifier else Modifier.padding(start = if (showNames) 12.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundBrandLogo(
                    glyph = exchange.shortName,
                    backgroundColor = exchange.color,
                    modifier = Modifier
                        .size(if (showNames) 30.dp else 24.dp)
                        .then(if (showNames) Modifier else Modifier.border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)),
                )
                if (showNames) {
                    Spacer(Modifier.width(5.dp))
                    Text(exchange.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
        if (!showNames && additional > 0) {
            Text(
                text = "+$additional",
                modifier = Modifier.padding(start = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
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
        shape = RoundedCornerShape(9.dp),
    ) {
        Text(
            text = "${if (positive) "+" else ""}${value.toString().replace('.', ',')}%",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoBorder),
    ) {
        content()
    }
}

@Composable
fun SparklineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .semantics { contentDescription = "Gráfico das últimas 24 horas com tendência de alta" },
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
    onBackground: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sincronizando mercado", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(CryptoOrangeSoft, -90f, 360f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                drawArc(CryptoOrange, -90f, 223.2f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            }
            Text("62%", color = CryptoOrange, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("620 de 1.000 moedas", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(14.dp))
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            drawRoundRect(CryptoOrangeSoft, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            drawRoundRect(CryptoOrange, size = Size(size.width * .62f, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        }
        Spacer(Modifier.height(18.dp))
        Text("Salvando dados no dispositivo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Você pode continuar usando o app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        PrimaryActionButton("Executar em segundo plano", onBackground, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Cancelar",
            modifier = Modifier.clickable(onClick = onCancel).padding(12.dp),
            color = CryptoOrange,
            fontWeight = FontWeight.SemiBold,
        )
    }
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
