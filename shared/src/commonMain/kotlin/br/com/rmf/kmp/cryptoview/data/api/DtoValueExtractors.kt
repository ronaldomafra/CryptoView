package br.com.rmf.kmp.cryptoview.data.api

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

