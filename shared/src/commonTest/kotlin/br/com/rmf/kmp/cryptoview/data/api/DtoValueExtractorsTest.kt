package br.com.rmf.kmp.cryptoview.data.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DtoValueExtractorsTest {
    @Test
    fun readsUsdQuoteFromObjectAndArrayShapes() {
        val objectQuote = Json.parseToJsonElement("""{"USD":{"price":42.5}}""")
        val arrayQuote = Json.parseToJsonElement("""[{"id":"2781","price":10.25}]""")

        assertEquals(42.5, objectQuote.usdQuote().doubleValue("price"))
        assertEquals(10.25, arrayQuote.usdQuote().doubleValue("price"))
    }
}
