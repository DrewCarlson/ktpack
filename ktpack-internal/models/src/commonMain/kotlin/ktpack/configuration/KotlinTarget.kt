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
    LINUX_X64(executableExt = ".kexe", libraryExt = ".klib", isNative = true),
}
