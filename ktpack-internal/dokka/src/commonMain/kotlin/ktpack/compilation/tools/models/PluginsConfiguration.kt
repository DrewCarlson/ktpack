package ktpack.compilation.tools.models


import kotlinx.serialization.Serializable

@Serializable
data class PluginsConfiguration(
    val fqPluginName: String,
    val serializationFormat: String,
    val values: String
)
