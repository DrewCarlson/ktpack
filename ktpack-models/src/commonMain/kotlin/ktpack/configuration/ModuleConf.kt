package ktpack.configuration

import kotlinx.serialization.Serializable

@Serializable
data class ModuleConf(
    val name: String,
    val version: String,
    val authors: List<String> = emptyList(),
    val description: String? = null,
    val readme: String? = null,
    val homepage: String? = null,
    val repository: String? = null,
    val license: String? = null,
    val publish: Boolean = false,
    val autobin: Boolean = true,
    val targets: List<KotlinTarget> = emptyList(),
    val kotlinVersion: String? = null,
    val dependencies: List<DependencyContainer> = emptyList()
)
