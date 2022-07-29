package ktpack.commands.kotlin

import ktfio.filePathSeparator
import ktpack.util.ARCH
import ktpack.util.KONAN_ROOT

object KotlincInstalls {

    private fun findNonNativeBin(version: String): String = buildString {
        append(KONAN_ROOT)
        append(filePathSeparator)
        append("kotlin-compiler-prebuilt-")
        append(version)
        append(filePathSeparator)
        append("bin")
        append(filePathSeparator)
    }

    fun findKotlinHome(version: String): String = buildString {
        append(KONAN_ROOT)
        append(filePathSeparator)
        append("kotlin-compiler-prebuilt-")
        append(version)
        append(filePathSeparator)
    }

    fun findKotlincJvm(version: String): String = buildString {
        append(findNonNativeBin(version))
        append("kotlinc-jvm")
        if (Platform.osFamily == OsFamily.WINDOWS) {
            append(".bat")
        }
    }

    fun findKotlincJs(version: String): String = buildString {
        append(findNonNativeBin(version))
        append("kotlinc-js")
        if (Platform.osFamily == OsFamily.WINDOWS) {
            append(".bat")
        }
    }

    fun findKotlincNative(version: String): String = buildString {
        val (major, minor, _) = version.split('.').map(String::toInt)
        append(KONAN_ROOT)
        append(filePathSeparator)
        append("kotlin-native-prebuilt-")
        when (Platform.osFamily) {
            OsFamily.MACOSX -> "macos"
            OsFamily.WINDOWS -> "windows"
            OsFamily.LINUX -> "linux"
            else -> error("Unsupported Host ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }.run(::append)
        append('-')
        if (major == 1 && minor >= 6) {
            append(ARCH)
            append('-')
        }
        append(version)
        append(filePathSeparator)
        append("bin")
        append(filePathSeparator)
        append("kotlinc-native")
        if (Platform.osFamily == OsFamily.WINDOWS) {
            append(".bat")
        }
    }
}
