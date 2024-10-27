package ktpack.configuration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class KotlinTarget(
    val executableExt: String,
    val libraryExt: String,
    val isNative: Boolean = false,
    val isJs: Boolean = false,
    val aliases: List<String>,
) {

    @SerialName("jvm")
    JVM(
        executableExt = ".jar",
        libraryExt = ".jar",
        aliases = listOf("jvm"),
    ),

    @SerialName("js_node")
    JS_NODE(
        executableExt = ".js",
        libraryExt = "",
        isJs = true,
        aliases = listOf("js", "jsnode"),
    ),

    @SerialName("js_browser")
    JS_BROWSER(
        executableExt = ".js",
        libraryExt = "",
        isJs = true,
        aliases = listOf("js", "jsbrowser"),
    ),

    @SerialName("macos_arm64")
    MACOS_ARM64(
        executableExt = ".kexe",
        libraryExt = ".klib",
        isNative = true,
        aliases = listOf("native", "macos", "posix", "macosarm64"),
    ),

    @SerialName("macos_x64")
    MACOS_X64(
        executableExt = ".kexe",
        libraryExt = ".klib",
        isNative = true,
        aliases = listOf("native", "macos", "posix", "macosx64"),
    ),

    @SerialName("mingw_x64")
    MINGW_X64(
        executableExt = ".exe",
        libraryExt = ".klib",
        isNative = true,
        aliases = listOf("native", "windows", "mingw", "mingwx64"),
    ),

    @SerialName("linux_arm64")
    LINUX_ARM64(
        executableExt = ".kexe",
        libraryExt = ".klib",
        isNative = true,
        aliases = listOf("native", "linux", "posix", "linuxarm64"),
    ),

    @SerialName("linux_x64")
    LINUX_X64(
        executableExt = ".kexe",
        libraryExt = ".klib",
        isNative = true,
        aliases = listOf("native", "linux", "posix", "linuxx64"),
    );

    companion object {

        fun targetsFromAlias(alias: String): List<KotlinTarget> {
            return entries.filter { target -> target.aliases.contains(alias) }
        }

        val ALL_MACOS = listOf(MACOS_X64, MACOS_ARM64)

        val ALL_WINDOWS = listOf(MINGW_X64)

        val ALL_LINUX = listOf(LINUX_X64, LINUX_ARM64)

        val ALL_JVM = listOf(JVM)
    }
}
