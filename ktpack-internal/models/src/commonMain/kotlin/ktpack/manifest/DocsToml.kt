package ktpack.manifest

import kotlinx.serialization.Serializable


@Serializable
data class DocsToml(
    val version: String? = null
)
