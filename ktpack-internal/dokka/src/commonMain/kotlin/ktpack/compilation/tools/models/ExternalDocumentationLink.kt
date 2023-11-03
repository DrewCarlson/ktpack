package ktpack.compilation.tools.models


import kotlinx.serialization.Serializable

@Serializable
data class ExternalDocumentationLink(
    val packageListUrl: String,
    val url: String
)
