package ktpack.toolchain.jdk

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import ktpack.CliContext
import ktpack.toolchain.ToolchainInstaller
import ktpack.toolchains.ToolchainInstallProgress
import ktpack.toolchains.ToolchainInstallResult
import ktpack.util.*
import okio.Path
import okio.Path.Companion.toPath

class JdkInstalls(
    private val context: CliContext,
) : ToolchainInstaller<JdkInstallDetails>(context.http) {

    private val config = context.config.jdk

    fun getDefaultJdk(): JdkInstallDetails? {
        return findJdk(
            checkNotNull(config.rootPath).toPath(),
            config.version,
            config.distribution,
        )
    }

    /**
     * Check if the [jdksRoot] contains any JDK install we recognize.
     */
    fun hasAnyJdks(jdksRoot: Path): Boolean {
        return jdksRoot.list().any { file ->
            val fileName = file.name // JDK folders use the `zulu-18.0.1` format
            val distribution = try {
                JdkDistribution.valueOf(fileName.substringBefore('-').replaceFirstChar(Char::uppercase))
            } catch (e: IllegalArgumentException) {
                null
            }

            distribution != null && fileName.substringAfter('-').split('.').size == 3
        }
    }

    /**
     * Find the [JdkInstallDetails] for the installation that best
     * matches the JDK [version] in [jdksRoot], and optionally matches
     * the [distribution] if provided.
     */
    fun findJdk(jdksRoot: Path, version: String, distribution: JdkDistribution? = null): JdkInstallDetails? {
        return distribution?.let { dist ->
            (jdksRoot / "${dist.name.lowercase()}-$version")
                .takeIf(Path::exists)
                ?.let(::createInstallDetails)
        } ?: discover(jdksRoot, distribution).firstOrNull { install ->
            install.version.startsWith(version) && (distribution == null || install.distribution == distribution)
        }
    }

    /**
     * Find all [JdkInstallDetails] for JDK installs in [jdksRoot].
     */
    fun discover(rootPath: Path, distribution: JdkDistribution? = null): List<JdkInstallDetails> {
        val distName = distribution?.name?.lowercase()
        return rootPath.list().mapNotNull { file ->
            if (distName != null && !file.name.startsWith(distName)) {
                null
            } else if (file.isDirectory() && file.list().isNotEmpty()) {
                createInstallDetails(file)
            } else {
                null
            }
        }
    }

    override fun discover(rootPath: Path): List<JdkInstallDetails> {
        return discover(rootPath, distribution = null)
    }

    suspend fun findAndInstallJdk(
        jdksRoot: Path,
        version: String,
        distribution: JdkDistribution,
        onProgress: (ToolchainInstallProgress) -> Unit,
    ): ToolchainInstallResult<JdkInstallDetails> {
        val (downloadUrl, archiveName, jdkVersionString) = when (distribution) {
            JdkDistribution.Zulu -> availableZuluVersions(version)
            JdkDistribution.Temurin -> availableTemurinVersions(version)
            JdkDistribution.Corretto -> availableCorrettoVersions(version)
            JdkDistribution.Jbr -> error("Jetbrains runtime is not supported.")
        }
        if (downloadUrl == null || archiveName == null || jdkVersionString == null) {
            return ToolchainInstallResult.NoMatchingVersion
        }
        val existingInstallation = findJdk(jdksRoot, jdkVersionString, distribution)
        if (existingInstallation != null) {
            return ToolchainInstallResult.AlreadyInstalled(existingInstallation)
        }

        val (result, tempExtractPath) = downloadAndExtract(downloadUrl, onProgress)
        if (result != null) {
            return result
        }

        val newJdkName = "${distribution.name.lowercase()}-$jdkVersionString"
        val newJdkFolder = jdksRoot / newJdkName
        return moveExtractedFiles(
            installDetails = checkNotNull(createInstallDetails(newJdkFolder)),
            extractPath = tempExtractPath,
            newFolderPath = newJdkFolder,
        )
    }

    private fun createInstallDetails(file: Path): JdkInstallDetails? {
        val fileName = file.name // JDK folders use the `zulu-18.0.1` format
        val distribution = try {
            JdkDistribution.valueOf(fileName.substringBefore('-').replaceFirstChar(Char::uppercase))
        } catch (e: IllegalArgumentException) {
            return null
        }
        val intellijManifestFilename = file / ".$fileName.intellij"
        return JdkInstallDetails(
            distribution = distribution,
            version = fileName.substringAfter('-'),
            intellijManifest = intellijManifestFilename.run {
                if (exists()) toString() else null
            },
            path = file.toString(),
            isActive = pathEnv.contains(fileName),
        )
    }

    private suspend fun availableTemurinVersions(
        jdkVersion: String,
    ): Triple<String?, String?, String?> {
        val osName = when (Platform.osFamily) {
            OsFamily.MACOSX -> "mac"
            OsFamily.LINUX -> "linux"
            OsFamily.WINDOWS -> "windows"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
        val arch = when (Platform.cpuArchitecture) {
            CpuArchitecture.ARM64 -> "aarch64"
            CpuArchitecture.X64 -> "x64"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
        val packageFilter = "jdk_${arch}_$osName"
        val majorVersion = jdkVersion.substringBefore('.').toInt()
        val releases = http.get("https://api.github.com/repos/adoptium/temurin$majorVersion-binaries/releases")
            .body<List<GhRelease>>()

        // Tag formats and version numbering are slightly different for Temurin 8 releases
        val tagFilter = if (majorVersion == 8) {
            // Parse and use the minor version if provided
            val versionParts = jdkVersion.split('.')
            "jdk${majorVersion}u${versionParts.getOrNull(2).orEmpty()}"
        } else {
            "jdk-$jdkVersion"
        }
        return releases.asSequence()
            .filter { release -> release.tagName.startsWith(tagFilter) }
            .mapNotNull { release ->
                release.assets.find { asset ->
                    asset.name.contains(packageFilter) && asset.name.endsWith(packageExtension)
                }?.let { asset ->
                    val jdkVersionString = if (release.name.startsWith("jdk8")) {
                        val (major, patch) = release.name.substringAfter("jdk").substringBefore('-').split('u')
                        "$major.0.$patch"
                    } else {
                        release.name.substringAfter("jdk-").substringBeforeLast("+")
                    }
                    Triple(asset.downloadUrl, asset.name, jdkVersionString)
                }
            }.firstOrNull() ?: Triple(null, null, null)
    }

    private suspend fun availableCorrettoVersions(
        jdkVersion: String,
    ): Triple<String?, String?, String?> {
        val osName = when (Platform.osFamily) {
            OsFamily.MACOSX -> "macosx"
            OsFamily.LINUX -> "linux"
            OsFamily.WINDOWS -> "windows"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
        val arch = when (Platform.cpuArchitecture) {
            CpuArchitecture.ARM64 -> "aarch64"
            CpuArchitecture.X64 -> "x64"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
        val majorVersion = jdkVersion.substringBefore('.').toInt()
        val releases = http.get("https://api.github.com/repos/corretto/corretto-$majorVersion/releases")
            .body<List<GhRelease>>()

        val tagFilter = if (majorVersion == 8) {
            // Parse and use the minor version if provided
            val versionParts = jdkVersion.split('.')
            "$majorVersion.${versionParts.getOrNull(2).orEmpty()}"
        } else {
            jdkVersion
        }
        val release = releases.asSequence()
            .filter { release -> release.tagName.startsWith(tagFilter) }
            .firstOrNull() ?: return Triple(null, null, null)
        val packageSuffix = if (Platform.osFamily == OsFamily.MACOSX) "" else "jdk"
        val packageName = "amazon-corretto-${release.tagName}-$osName-$arch-${packageSuffix}$packageExtension"
        val downloadUrl = "https://corretto.aws/downloads/resources/${release.tagName}/$packageName"
        val actualJdkVersion = if (majorVersion == 8) {
            val tagParts = release.tagName.split('.')
            "${tagParts[0]}.0.${tagParts[1]}"
        } else {
            release.tagName.split('.').take(3).joinToString(".")
        }
        return Triple(downloadUrl, packageName, actualJdkVersion)
    }

    private suspend fun availableZuluVersions(jdkVersion: String): Triple<String?, String?, String?> {
        val osName = when (Platform.osFamily) {
            OsFamily.MACOSX -> "macosx"
            OsFamily.LINUX -> "linux"
            OsFamily.WINDOWS -> "win"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
        val arch = when (Platform.cpuArchitecture) {
            CpuArchitecture.ARM64 -> "aarch64"
            CpuArchitecture.X64 -> "x64"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
        val packageSuffix = "${osName}_$arch$packageExtension"
        val versionFilter = "ca-jdk$jdkVersion" // NOTE: Community Availability filter

        val body = http.get("https://cdn.azul.com/zulu/bin/").bodyAsText()
        val regex = """<a href="(zulu[A-Za-z0-9._-]+)">""".toRegex()
        return regex.findAll(body).map { it.groupValues[1] }
            .mapNotNull { releaseFileName ->
                releaseFileName.takeIf { it.endsWith(packageSuffix) && it.contains(versionFilter) }
                    ?: return@mapNotNull null
                val jdkVersionString = releaseFileName.substringAfter("jdk").substringBefore('-')

                Triple("https://cdn.azul.com/zulu/bin/$releaseFileName", releaseFileName, jdkVersionString)
            }
            .sortedByDescending { (_, _, jdkVersionString) ->
                val (major, minor, patch) = jdkVersionString.split('.').map(String::toInt)
                (major * 100000) + (minor * 1000) + patch
            }
            .firstOrNull() ?: Triple(null, null, null)
    }
}
