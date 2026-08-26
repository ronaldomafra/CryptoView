package br.com.rmf.kmp.cryptoview.ui.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class UiFormattersTest {
    @Test
    fun formatsUsdUsingBrazilianSeparators() {
        assertEquals("$ 78.837,05", formatUsd(78_837.05))
        assertEquals("$ 0,005169", formatUsd(0.00516863))
        assertEquals("—", formatUsd(null))
    }

    @Test
    fun formatsCompactValuesAndVariation() {
        assertEquals("$ 18,4 bi", formatCompactUsd(18_440_000_000.0))
        assertEquals("+0,05%", formatPercentage(0.05168631))
        assertEquals("-1,04%", formatPercentage(-1.03606503))
    }

    @Test
    fun describesUpdateAge() {
        val now = 10_000_000L
        assertEquals("Atualizado agora", formatRelativeUpdate(now - 30_000L, now))
        assertEquals("Atualizado há 1 min", formatRelativeUpdate(now - 60_000L, now))
        assertEquals("Atualizado há 2 horas", formatRelativeUpdate(now - 120 * 60_000L, now))
    }
}
