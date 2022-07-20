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
    val license: String? = null,
    val publish: Boolean = false,
    val autobin: Boolean = true,
    val targets: List<Target>,
    val kotlinVersion: String? = null,
)

enum class Target {
    COMMON_ONLY,
    JVM,
    JS_NODE,
    JS_BROWSER,
    MACOS_ARM64,
    MACOS_X64,
    WINDOWS_X64,
    LINUX_X64,
}
