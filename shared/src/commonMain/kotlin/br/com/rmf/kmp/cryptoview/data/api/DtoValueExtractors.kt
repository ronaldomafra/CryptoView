package br.com.rmf.kmp.cryptoview.data.api

import br.com.rmf.kmp.cryptoview.domain.model.api.CoinHistoryDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonElement?.usdQuote(): JsonObject? = when (this) {
    is JsonObject -> {
        (this["USD"] as? JsonObject)
            ?: (this["2781"] as? JsonObject)
            ?: values.firstOrNull() as? JsonObject
    }
    is JsonArray -> firstOrNull { element ->
        val objectValue = element as? JsonObject ?: return@firstOrNull false
        objectValue["symbol"]?.jsonPrimitive?.content == "USD" ||
            objectValue["id"]?.jsonPrimitive?.content == "2781"
    }?.jsonObject ?: firstOrNull() as? JsonObject
    else -> null
}

internal fun JsonObject?.doubleValue(name: String): Double? =
    this?.get(name)?.jsonPrimitive?.doubleOrNull

internal fun JsonObject?.stringValue(name: String): String? =
    this?.get(name)?.jsonPrimitive?.content

internal fun JsonElement.coinHistoryDto(coinId: Long): CoinHistoryDto? {
    val candidate = when (this) {
        is JsonArray -> firstOrNull { element ->
            (element as? JsonObject)?.get("id")?.jsonPrimitive?.content == coinId.toString()
        } ?: firstOrNull()
        is JsonObject -> when {
            containsKey("quotes") -> this
            containsKey(coinId.toString()) -> get(coinId.toString())
            else -> values.firstOrNull { element ->
                (element as? JsonObject)?.get("id")?.jsonPrimitive?.content == coinId.toString()
            } ?: values.firstOrNull()
        }
        else -> null
    } ?: return null

    return runCatching {
        cryptoNetworkJson.decodeFromString<CoinHistoryDto>(candidate.toString())
    }.getOrNull()
}
