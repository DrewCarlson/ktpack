package ktpack.compilation.tools.models


import kotlinx.serialization.Serializable

@Serializable
data class DependentSourceSet(
    val scopeId: String,
    val sourceSetName: String
)
