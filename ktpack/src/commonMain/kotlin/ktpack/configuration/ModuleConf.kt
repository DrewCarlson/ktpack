package ktpack.configuration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
enum class Target {
    @SerialName("jvm")
    JVM,
    @SerialName("js_node")
    JS_NODE,
    @SerialName("js_browser")
    JS_BROWSER,
    @SerialName("macos_arm64")
    MACOS_ARM64,
    @SerialName("macos_x64")
    MACOS_X64,
    @SerialName("windows_x64")
    WINDOWS_X64,
    @SerialName("linux_x64")
    LINUX_X64,
}
