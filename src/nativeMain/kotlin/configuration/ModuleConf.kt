package ktpack.configuration

data class ModuleConf(
    val name: String,
    val version: String,
    val authors: List<String>,
    val description: String? = null,
    val keywords: List<String>? = null,
    val readme: String? = null,
    val homepage: String? = null,
    val repository: String? = null,
    val publish: Boolean = false,
    val autobin: Boolean = true,
)
