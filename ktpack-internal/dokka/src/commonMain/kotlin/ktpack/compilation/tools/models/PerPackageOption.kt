package ktpack.compilation.tools.models


import kotlinx.serialization.Serializable

@Serializable
data class PerPackageOption(
    val documentedVisibilities: List<DocumentedVisibilities>,
    val matchingRegex: String,
    val reportUndocumented: Boolean,
    val skipDeprecated: Boolean,
    val suppress: Boolean
)
