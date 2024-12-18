package ktpack.toolchain.nodejs

import io.ktor.client.*
import kotlinx.io.files.Path
import ktpack.KtpackUserConfig
import ktpack.configuration.KotlinTarget
import ktpack.toolchain.ToolchainInstaller
import ktpack.toolchain.ToolchainInstallProgress
import ktpack.toolchain.ToolchainInstallResult
import ktpack.util.PlatformUtils
import ktpack.util.exists
import ktpack.util.isDirectory
import ktpack.util.list

class NodejsInstalls(
    private val config: KtpackUserConfig,
    http: HttpClient,
) : ToolchainInstaller<NodejsInstallDetails>(http) {

    fun getDefaultNodejs(): NodejsInstallDetails? {
        return findNodejs(
            Path(config.nodejs.rootPath),
            config.nodejs.version
        )
    }

    override fun discover(rootPath: Path): List<NodejsInstallDetails> {
        return rootPath.list().mapNotNull { file ->
            if (file.isDirectory() && file.list().isNotEmpty()) {
                createInstallDetails(file)
            } else {
                null
            }
        }
    }

    suspend fun findAndInstall(
        rootPath: Path,
        version: String,
        onProgress: (ToolchainInstallProgress) -> Unit,
    ): ToolchainInstallResult<NodejsInstallDetails> {
        val existingInstall = findNodejs(rootPath, version)
        if (existingInstall != null) {
            return ToolchainInstallResult.AlreadyInstalled(existingInstall)
        }

        val downloadUrl = getReleaseUrl(version)
        val (result, tempExtractPath) = downloadAndExtract(downloadUrl, onProgress)
        if (result != null) {
            return result
        }

        val newNodejsName = tempExtractPath.name.split('-')[1]
        val newNodejsFolder = Path(rootPath, newNodejsName)
        return moveExtractedFiles(
            installDetails = createInstallDetails(newNodejsFolder),
            extractPath = tempExtractPath,
            newFolderPath = newNodejsFolder
        )
    }

    fun findNodejsExe(version: String): Path? {
        val exe = if (PlatformUtils.getHostTarget() == KotlinTarget.MINGW_X64) {
            "node.exe"
        } else {
            "node"
        }
        return Path(config.nodejs.rootPath, "v$version", exe)
            .takeIf(Path::exists)
    }

    fun findNodejs(version: String): NodejsInstallDetails? {
        return Path(config.nodejs.rootPath, "v$version")
            .takeIf(Path::exists)
            ?.let(::createInstallDetails)
    }

    fun findNodejs(nodejsRoot: Path, version: String): NodejsInstallDetails? {
        return Path(nodejsRoot, "v$version")
            .takeIf(Path::exists)
            ?.let(::createInstallDetails)
    }

    private fun getReleaseUrl(version: String): String {
        val targetSuffix = when (PlatformUtils.getHostTarget()) {
            KotlinTarget.MACOS_ARM64 -> "-darwin-arm64.tar.gz"
            KotlinTarget.MACOS_X64 -> "-darwin-x64.tar.gz"
            KotlinTarget.MINGW_X64 -> "-win-x64.zip"
            KotlinTarget.LINUX_X64 -> "-linux-x64.tar.xz"
            KotlinTarget.LINUX_ARM64, //"-linux-arm64.tar.xz"
            KotlinTarget.JVM,
            KotlinTarget.JS_NODE,
            KotlinTarget.JS_BROWSER -> error("Unsupported host architecture: ${PlatformUtils.getHostSupportedTargets()}")
        }
        return "https://nodejs.org/dist/v${version}/node-v${version}$targetSuffix"
    }

    private fun createInstallDetails(path: Path): NodejsInstallDetails {
        return NodejsInstallDetails(
            version = path.name.trim('v'),
            path = path.toString(),
            isActive = pathEnv.contains(path.toString()),
        )
    }
}
