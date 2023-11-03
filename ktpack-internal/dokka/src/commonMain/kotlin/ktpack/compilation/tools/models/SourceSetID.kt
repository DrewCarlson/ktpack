package ktpack.compilation.tools.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SourceSetID(
    val scopeId: String,
    val sourceSetName: String
)
