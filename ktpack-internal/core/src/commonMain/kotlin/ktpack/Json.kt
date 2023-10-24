package ktpack

import kotlinx.serialization.json.Json

@SharedImmutable
val json = Json {
    ignoreUnknownKeys = true
}
