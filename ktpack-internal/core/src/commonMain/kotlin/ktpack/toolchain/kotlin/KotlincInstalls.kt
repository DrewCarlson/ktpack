package ktpack.toolchain.kotlin

import io.ktor.client.call.*
import io.ktor.client.request.*
import ktpack.CliContext
import ktpack.configuration.KotlinTarget
import ktpack.toolchain.ToolchainInstaller
import ktpack.toolchains.ToolchainInstallProgress
import ktpack.toolchains.ToolchainInstallResult
import ktpack.util.*
import okio.Path
import okio.Path.Companion.DIRECTORY_SEPARATOR
import okio.Path.Companion.toPath

private val FILE_VERSION_REGEX = """\d+\.\d+\.\d+(?:-\w+)?""".toRegex()

data class KotlincInstalls(
    private val context: CliContext
): ToolchainInstaller<KotlinInstallDetails>(context.http) {

    enum class CompilerType {
        JVM, NATIVE
    }

    suspend fun getCompilerReleases(page: Int): List<GhTag> {
        return context.http.get("https://api.github.com/repos/Jetbrains/kotlin/tags?per_page=100") {
            parameter("page", page)
        }.body()
    }

    override fun discover(rootPath: Path): List<KotlinInstallDetails> {
        return rootPath.list().mapNotNull { file ->
            val fileName = file.name
            when {
                !file.isDirectory() || file.list().isEmpty() -> null
                fileName.startsWith("kotlin-compiler-prebuilt-") -> {
                    val (version) = checkNotNull(FILE_VERSION_REGEX.find(fileName)).groupValues
                    createInstallDetails(file, CompilerType.JVM, version)
                }

                fileName.startsWith("kotlin-native-prebuilt-") -> {
                    val (version) = checkNotNull(FILE_VERSION_REGEX.find(fileName)).groupValues
                    createInstallDetails(file, CompilerType.NATIVE, version)
                }

                else -> null
            }
        }
    }

    private fun createInstallDetails(
        file: Path,
        type: CompilerType,
        version: String,
    ): KotlinInstallDetails {
        return KotlinInstallDetails(
            version = version,
            path = file.toString(),
            type = type,
            isActive = pathEnv.contains(file.name),
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
        val (major, minor, _) = version.split('.').mapNotNull(String::toIntOrNull)
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

    suspend fun findAndInstallKotlin(
        kotlincRoot: Path,
        version: String,
        compilerType: CompilerType,
        onProgress: (ToolchainInstallProgress) -> Unit,
    ): ToolchainInstallResult<KotlinInstallDetails> {
        val downloadUrl = getReleaseUrl(compilerType, version)
        val existingInstallation = findKotlin(kotlincRoot, version, compilerType)
        if (existingInstallation != null) {
            return ToolchainInstallResult.AlreadyInstalled(existingInstallation)
        }

        val (result, tempExtractPath) = downloadAndExtract(downloadUrl, onProgress)
        if (result != null) {
            return result
        }
        val newKotlinName = tempExtractPath.name
            .replace("kotlin-compiler", "kotlin-compiler-prebuilt")
            .replace("kotlin-native", "kotlin-native-prebuilt")
        val newKotlinFolder = kotlincRoot / newKotlinName
        val installDetails = createInstallDetails(newKotlinFolder, compilerType, version)
        return moveExtractedFiles(
            installDetails = installDetails,
            newFolderPath = newKotlinFolder,
            extractPath = tempExtractPath
        )
    }

    private fun getReleaseUrl(compilerType: CompilerType, version: String): String {
        return buildString {
            append("https://github.com/JetBrains/kotlin/releases/download/v")
            append(version)
            append('/')
            when (compilerType) {
                CompilerType.JVM -> {
                    append("kotlin-compiler-")
                    append(version)
                    append(".zip")
                }

                CompilerType.NATIVE -> {
                    append("kotlin-native-")
                    when (PlatformUtils.getHostTarget()) {
                        KotlinTarget.MACOS_ARM64 -> {
                            append("macos-aarch64-")
                            append(version)
                            append(".tar.gz")
                        }

                        KotlinTarget.MACOS_X64 -> {
                            append("macos-x86_64-")
                            append(version)
                            append(".tar.gz")
                        }

                        KotlinTarget.MINGW_X64 -> {
                            val (major, minor, _) = version.split('.')
                                .mapNotNull { it.toIntOrNull() }
                            if (major == 1 && minor < 6) {
                                append("windows-")
                            } else {
                                append("windows-x86_64-")
                            }
                            append(version)
                            append(".zip")
                        }

                        KotlinTarget.LINUX_X64 -> {
                            append("linux-x86_64-")
                            append(version)
                            append(".tar.gz")
                        }

                        else -> error("Unsupported KotlinTarget: ${PlatformUtils.getHostTarget()}")
                    }
                }
            }
        }
    }

    fun findKotlin(
        kotlincRoot: Path,
        version: String,
        compilerType: CompilerType,
    ): KotlinInstallDetails? {
        val folderName = when (compilerType) {
            CompilerType.JVM -> findNonNativeBin(version).toPath().parent!!.name
            CompilerType.NATIVE -> findKotlincNative(version).toPath().parent!!.parent!!.name
        }
        return (kotlincRoot / folderName)
            .takeIf(Path::exists)
            ?.let { kotlinFolder ->
                createInstallDetails(
                    file = kotlinFolder,
                    type = compilerType,
                    version = version,
                )
            }
    }
}
