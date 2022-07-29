package ktpack.configuration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class Target(
    val isNative: Boolean
) {
    @SerialName("jvm")
    JVM(false),
    @SerialName("js_node")
    JS_NODE(false),
    @SerialName("js_browser")
    JS_BROWSER(false),
    @SerialName("macos_arm64")
    MACOS_ARM64(true),
    @SerialName("macos_x64")
    MACOS_X64(true),
    @SerialName("mingw_x64")
    MINGW_X64(true),
    @SerialName("linux_x64")
    LINUX_X64(true);
}
