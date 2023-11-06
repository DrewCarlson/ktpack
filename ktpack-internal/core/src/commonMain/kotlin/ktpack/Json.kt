package ktpack

import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
}

val jsonPretty = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}
