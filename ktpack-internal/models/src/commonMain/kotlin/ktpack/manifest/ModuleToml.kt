package ktpack.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ktpack.configuration.KotlinTarget

@Serializable
data class ModuleToml(
    val name: String,
    val version: String? = null,
    val authors: List<String> = emptyList(),
    val description: String? = null,
    val readme: String? = null,
    val homepage: String? = null,
    val repository: String? = null,
    val license: String? = null,
    val publish: Boolean = false,
    val autobin: Boolean = true,
    val targets: List<KotlinTarget> = emptyList(),
    @SerialName("kotlin_version")
    val kotlinVersion: String? = null,
)
