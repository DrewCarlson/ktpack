package ktpack.compilation.tools.models


import kotlinx.serialization.Serializable

@Serializable
data class SourceLink(
    val localDirectory: String,
    val remoteLineSuffix: String,
    val remoteUrl: String
)
