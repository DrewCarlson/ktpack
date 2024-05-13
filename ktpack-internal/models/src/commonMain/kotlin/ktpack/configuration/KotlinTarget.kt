package ktpack.configuration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class KotlinTarget(
    val executableExt: String,
    val libraryExt: String,
    val isNative: Boolean = false,
    val isJs: Boolean = false,
) {

    @SerialName("jvm")
    JVM(executableExt = ".jar", libraryExt = ".jar"),

    @SerialName("js_node")
    JS_NODE(executableExt = ".js", libraryExt = "", isJs = true),

    @SerialName("js_browser")
    JS_BROWSER(executableExt = ".js", libraryExt = "", isJs = true),

    @SerialName("macos_arm64")
    MACOS_ARM64(executableExt = ".kexe", libraryExt = ".klib", isNative = true),

    @SerialName("macos_x64")
    MACOS_X64(executableExt = ".kexe", libraryExt = ".klib", isNative = true),

    @SerialName("mingw_x64")
    MINGW_X64(executableExt = ".exe", libraryExt = ".klib", isNative = true),

    @SerialName("linux_arm64")
    LINUX_ARM64(executableExt = ".kexe", libraryExt = ".klib", isNative = true),

    @SerialName("linux_x64")
    LINUX_X64(executableExt = ".kexe", libraryExt = ".klib", isNative = true);

    companion object {

        fun targetsFromAlias(alias: String): List<KotlinTarget> {
            return targetAliases.mapNotNull { (key, aliases) ->
                key.takeIf { aliases.contains(alias) }
            }
        }
        private val targetAliases = mapOf(
            JVM to listOf("common", "jvm"),
            JS_NODE to listOf("common", "js", "jsnode"),
            JS_BROWSER to listOf("common", "js", "jsbrowser"),
            MACOS_ARM64 to listOf("common", "native", "macos", "posix", "macosarm64"),
            MACOS_X64 to listOf("common", "native", "macos", "posix", "macosx64"),
            MINGW_X64 to listOf("common", "native", "windows", "mingw", "mingwx64"),
            LINUX_ARM64 to listOf("common", "native", "linux", "posix", "linuxarm64"),
            LINUX_X64 to listOf("common", "native", "linux", "posix", "linuxx64"),
        )
    }
}
