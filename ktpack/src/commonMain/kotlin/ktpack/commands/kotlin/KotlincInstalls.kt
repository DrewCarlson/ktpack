package ktpack.commands.kotlin

import kotlinx.cinterop.toKString
import ktpack.CliContext
import ktpack.util.ARCH
import ktpack.util.isDirectory
import ktpack.util.list
import okio.Path
import okio.Path.Companion.DIRECTORY_SEPARATOR
import platform.posix.getenv

data class KotlincInstalls(private val context: CliContext) {

    enum class CompilerType {
        JVM, NATIVE
    }

    data class InstallDetails(
        val version: String,
        val path: String,
        val type: CompilerType,
        val isActive: Boolean,
    )

    fun discover(kotlincRoot: Path): List<InstallDetails> {
        val pathEnv = getenv("PATH")?.toKString().orEmpty()
        return kotlincRoot.list().mapNotNull { file ->
            val fileName = file.name
            when {
                !file.isDirectory() || file.list().isEmpty() -> null
                fileName.startsWith("kotlin-compiler-prebuilt-") ->
                    createInstallDetails(file, fileName, CompilerType.JVM, pathEnv)
                fileName.startsWith("kotlin-native-prebuilt-") ->
                    createInstallDetails(file, fileName, CompilerType.NATIVE, pathEnv)
                else -> null
            }
        }
    }

    private fun createInstallDetails(
        file: Path,
        fileName: String,
        type: CompilerType,
        pathEnv: String,
    ): InstallDetails {
        val version = fileName.split('-').last()
        val path = file.name
        return InstallDetails(
            version = version,
            path = path,
            type = type,
            isActive = pathEnv.contains(path),
        )
    }

    private fun findNonNativeBin(version: String): String = buildString {
        append(context.config.kotlin.rootPath)
        append(DIRECTORY_SEPARATOR)
        append("kotlin-compiler-prebuilt-")
        append(version)
        append(DIRECTORY_SEPARATOR)
        append("bin")
        append(DIRECTORY_SEPARATOR)
    }

    fun findKotlinHome(version: String): String = buildString {
        append(context.config.kotlin.rootPath)
        append(DIRECTORY_SEPARATOR)
        append("kotlin-compiler-prebuilt-")
        append(version)
    }

    fun findKotlinBin(binName: String, version: String): String = buildString {
        append(findNonNativeBin(version))
        append(binName)
        if (Platform.osFamily == OsFamily.WINDOWS) {
            append(".bat")
        }
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
        append(context.config.kotlin.rootPath)
        append(DIRECTORY_SEPARATOR)
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
        append(DIRECTORY_SEPARATOR)
        append("bin")
        append(DIRECTORY_SEPARATOR)
        append("kotlinc-native")
        if (Platform.osFamily == OsFamily.WINDOWS) {
            append(".bat")
        }
    }
}
