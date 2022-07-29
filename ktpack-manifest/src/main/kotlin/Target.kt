import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    @SerialName("mingw_x64")
    MINGW_X64,

    @SerialName("linux_x64")
    LINUX_X64,
}