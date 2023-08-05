package ktpack.configuration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class KotlinTarget(
    val isNative: Boolean = false,
    val isJs: Boolean = false,
) {
    @SerialName("jvm")
    JVM,

    @SerialName("js_node")
    JS_NODE(isJs = true),

    @SerialName("js_browser")
    JS_BROWSER(isJs = true),

    @SerialName("macos_arm64")
    MACOS_ARM64(isNative = true),

    @SerialName("macos_x64")
    MACOS_X64(isNative = true),

    @SerialName("mingw_x64")
    MINGW_X64(isNative = true),

    @SerialName("linux_arm64")
    LINUX_ARM64(isNative = true),

    @SerialName("linux_x64")
    LINUX_X64(isNative = true),
}
