package br.com.rmf.kmp.cryptoview.data.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DtoValueExtractorsTest {
    @Test
    fun readsUsdQuoteFromObjectAndArrayShapes() {
        val objectQuote = Json.parseToJsonElement("""{"USD":{"price":42.5}}""")
        val arrayQuote = Json.parseToJsonElement("""[{"id":"2781","price":10.25}]""")

        assertEquals(42.5, objectQuote.usdQuote().doubleValue("price"))
        assertEquals(10.25, arrayQuote.usdQuote().doubleValue("price"))
    }

    @Test
    fun readsHistoricalCoinFromDirectMapAndArrayShapes() {
        val direct = Json.parseToJsonElement(
            """{"id":1,"name":"Bitcoin","symbol":"BTC","quotes":[{"timestamp":"2026-08-26T10:00:00Z","quote":{"USD":{"price":100.0}}}]}""",
        )
        val mapped = Json.parseToJsonElement(
            """{"1":{"id":1,"name":"Bitcoin","symbol":"BTC","quotes":[{"timestamp":"2026-08-26T11:00:00Z","quote":{"USD":{"price":101.0}}}]}}""",
        )
        val array = Json.parseToJsonElement(
            """[{"id":1,"name":"Bitcoin","symbol":"BTC","quotes":[{"timestamp":"2026-08-26T12:00:00Z","quote":{"USD":{"price":102.0}}}]}]""",
        )

        assertEquals(100.0, direct.coinHistoryDto(1)?.quotes?.single()?.quote.usdQuote().doubleValue("price"))
        assertEquals(101.0, mapped.coinHistoryDto(1)?.quotes?.single()?.quote.usdQuote().doubleValue("price"))
        assertEquals(102.0, array.coinHistoryDto(1)?.quotes?.single()?.quote.usdQuote().doubleValue("price"))
        assertNull(Json.parseToJsonElement("42").coinHistoryDto(1))
    }

    @Test
    fun returnsNullWhenHistoricalShapeDoesNotContainACoin() {
        assertNull(Json.parseToJsonElement("{}").coinHistoryDto(1))
        assertNull(Json.parseToJsonElement("[]").coinHistoryDto(1))
    }
}
