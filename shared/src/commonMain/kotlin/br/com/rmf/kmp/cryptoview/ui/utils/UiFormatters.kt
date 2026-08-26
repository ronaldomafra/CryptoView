package br.com.rmf.kmp.cryptoview.ui.utils

import kotlin.math.abs
import kotlin.math.round

fun formatUsd(value: Double?): String {
    if (value == null || !value.isFinite()) return "—"
    val decimals = when {
        abs(value) >= 1.0 -> 2
        abs(value) >= 0.01 -> 4
        else -> 6
    }
    return "$ ${formatDecimal(value, decimals)}"
}

fun formatCompactUsd(value: Double?): String {
    if (value == null || !value.isFinite()) return "—"
    val absolute = abs(value)
    return when {
        absolute >= 1_000_000_000_000 -> "$ ${formatDecimal(value / 1_000_000_000_000, 1)} tri"
        absolute >= 1_000_000_000 -> "$ ${formatDecimal(value / 1_000_000_000, 1)} bi"
        absolute >= 1_000_000 -> "$ ${formatDecimal(value / 1_000_000, 1)} mi"
        absolute >= 1_000 -> "$ ${formatDecimal(value / 1_000, 1)} mil"
        else -> formatUsd(value)
    }
}

fun formatPercentage(value: Double): String {
    if (!value.isFinite()) return "—"
    val prefix = if (value >= 0.0) "+" else ""
    return "$prefix${formatDecimal(value, 2)}%"
}

fun formatRelativeUpdate(fetchedAtMillis: Long?, nowMillis: Long): String {
    if (fetchedAtMillis == null) return "Dados locais"
    val elapsedMinutes = ((nowMillis - fetchedAtMillis).coerceAtLeast(0L) / 60_000L)
    return when {
        elapsedMinutes < 1 -> "Atualizado agora"
        elapsedMinutes == 1L -> "Atualizado há 1 min"
        elapsedMinutes < 60 -> "Atualizado há $elapsedMinutes min"
        elapsedMinutes < 120 -> "Atualizado há 1 hora"
        elapsedMinutes < 1_440 -> "Atualizado há ${elapsedMinutes / 60} horas"
        elapsedMinutes < 2_880 -> "Atualizado há 1 dia"
        else -> "Atualizado há ${elapsedMinutes / 1_440} dias"
    }
}

private fun formatDecimal(value: Double, decimals: Int): String {
    val factor = powerOfTen(decimals)
    val scaled = round(abs(value) * factor).toLong()
    val integer = scaled / factor
    val fraction = scaled % factor
    val groupedInteger = integer.toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
    val sign = if (value < 0.0 && scaled != 0L) "-" else ""
    if (decimals == 0) return sign + groupedInteger
    return "$sign$groupedInteger,${fraction.toString().padStart(decimals, '0')}"
}

private fun powerOfTen(exponent: Int): Long {
    var result = 1L
    repeat(exponent) { result *= 10L }
    return result
}
