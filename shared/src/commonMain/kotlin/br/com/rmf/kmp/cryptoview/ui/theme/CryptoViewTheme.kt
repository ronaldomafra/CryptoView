package br.com.rmf.kmp.cryptoview.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CryptoOrange = Color(0xFFF4510B)
val CryptoOrangeSoft = Color(0xFFFFE8DC)
val CryptoBackground = Color(0xFFFFFCFA)
val CryptoSurface = Color(0xFFFFFFFF)
val CryptoInk = Color(0xFF15191D)
val CryptoMuted = Color(0xFF747474)
val CryptoBorder = Color(0xFFE4E0DD)
val CryptoPositive = Color(0xFF07862D)
val CryptoPositiveSoft = Color(0xFFE3F4E7)
val CryptoNegative = Color(0xFFD71920)
val CryptoNegativeSoft = Color(0xFFFFE4E4)

private val LightColors = lightColorScheme(
    primary = CryptoOrange,
    onPrimary = Color.White,
    primaryContainer = CryptoOrangeSoft,
    onPrimaryContainer = Color(0xFF812500),
    background = CryptoBackground,
    onBackground = CryptoInk,
    surface = CryptoSurface,
    onSurface = CryptoInk,
    surfaceVariant = Color(0xFFF7F4F2),
    onSurfaceVariant = CryptoMuted,
    outline = CryptoBorder,
    error = CryptoNegative,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8B55),
    onPrimary = Color(0xFF4E1700),
    primaryContainer = Color(0xFF742800),
    onPrimaryContainer = Color(0xFFFFDBCB),
    background = Color(0xFF151311),
    onBackground = Color(0xFFF4EFEC),
    surface = Color(0xFF211E1C),
    onSurface = Color(0xFFF4EFEC),
    surfaceVariant = Color(0xFF302B28),
    onSurfaceVariant = Color(0xFFD1C8C3),
    outline = Color(0xFF544C47),
    error = Color(0xFFFFB4AB),
)

private val CryptoTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun CryptoViewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CryptoTypography,
        content = content,
    )
}
