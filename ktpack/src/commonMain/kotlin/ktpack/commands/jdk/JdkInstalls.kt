package ktpack.commands.jdk

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flowOn
import ktfio.File
import ktfio.appendBytes
import ktfio.filePathSeparator
import ktfio.nestedFile
import ktpack.CliContext
import ktpack.util.*
import platform.posix.getenv

data class InstallationDetails(
    val distribution: JdkDistribution,
    val version: String,
    val intellijManifest: String?,
    val path: String,
    val isActive: Boolean,
) {
    val isIntellijInstall: Boolean = !intellijManifest.isNullOrBlank()
}

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

class JdkInstalls(
    private val context: CliContext,
) {

    private val config = context.config.jdk

    private val packageExtension by lazy {
        when (Platform.osFamily) {
            OsFamily.WINDOWS -> ".zip"
            OsFamily.LINUX, OsFamily.MACOSX -> ".tar.gz"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
    }

    fun getDefaultJdk(): InstallationDetails? {
        return findJdk(
            File(checkNotNull(config.rootPath)),
            config.version,
            config.distribution,
        )
    }

    /**
     * Check if the [jdksRoot] contains any JDK install we recognize.
     */
    fun hasAnyJdks(jdksRoot: File): Boolean {
        return jdksRoot.listFiles().any { file ->
            val fileName = file.getName() // JDK folders use the `zulu-18.0.1` format
            val distribution = try {
                JdkDistribution.valueOf(fileName.substringBefore('-').replaceFirstChar(Char::uppercase))
            } catch (e: IllegalArgumentException) {
                null
            }

            distribution != null && fileName.substringAfter('-').split('.').size == 3
        }
    }

    /**
     * Find the [InstallationDetails] for the installation that best
     * matches the JDK [version] in [jdksRoot], and optionally matches
     * the [distribution] if provided.
     */
    fun findJdk(jdksRoot: File, version: String, distribution: JdkDistribution? = null): InstallationDetails? {
        return distribution?.let { dist ->
            jdksRoot.nestedFile("${dist.name.lowercase()}-$version")
                .takeIf(File::exists)
                ?.let { jdkFolder ->
                    val pathEnv = getenv("PATH")?.toKString().orEmpty()
                    createInstallationDetails(jdkFolder, pathEnv)
                }
        } ?: discover(jdksRoot, distribution).firstOrNull { install ->
            install.version.startsWith(version) && (distribution == null || install.distribution == distribution)
        }
    }

    /**
     * Find all [InstallationDetails] for JDK installs in [jdksRoot].
     */
    fun discover(jdksRoot: File, distribution: JdkDistribution? = null): List<InstallationDetails> {
        val pathEnv = getenv("PATH")?.toKString().orEmpty()
        val distName = distribution?.name?.lowercase()
        return jdksRoot.listFiles().mapNotNull { file ->
            if (distName != null && !file.getName().startsWith(distName)) {
                null
            } else if (file.isDirectory() && file.list().isNotEmpty()) {
                createInstallationDetails(file, pathEnv)
            } else {
                null
            }
        }
    }

    suspend fun findAndInstallJdk(
        http: HttpClient,
        jdksRoot: File,
        version: String,
        distribution: JdkDistribution,
        onProgress: (JdkInstallProgress) -> Unit,
    ): JdkInstallResult {
        val pathEnv = getenv("PATH")?.toKString().orEmpty()
        val (downloadUrl, archiveName, jdkVersionString) = when (distribution) {
            JdkDistribution.Zulu -> availableZuluVersions(http, version)
            JdkDistribution.Temurin -> availableTemurinVersions(http, version)
            JdkDistribution.Corretto -> availableCorrettoVersions(http, version)
        }
        if (downloadUrl == null || archiveName == null || jdkVersionString == null) {
            return JdkInstallResult.NoMatchingVersion
        }
        val existingInstallation = findJdk(jdksRoot, jdkVersionString, distribution)
        if (existingInstallation != null) {
            return JdkInstallResult.AlreadyInstalled(existingInstallation)
        }

        val tempArchiveFile = TEMP_DIR.nestedFile(archiveName)
        val tempExtractedFolder = TEMP_DIR.nestedFile(archiveName.substringBeforeLast(packageExtension))
        if (!tempArchiveFile.createNewFile()) {
            return JdkInstallResult.FileIOError(tempArchiveFile, "Unable to create temp file")
        }

        val response = try {
            http.downloadPackage(downloadUrl, onProgress, tempArchiveFile)
        } catch (e: IOException) {
            return JdkInstallResult.DownloadError(downloadUrl, null, e)
        }
        if (!response.status.isSuccess()) {
            return JdkInstallResult.DownloadError(downloadUrl, response, null)
        }

        val compressor = if (Platform.osFamily == OsFamily.WINDOWS) Zip else Tar
        val archivePath = tempArchiveFile.getAbsolutePath()
        val fileCount = compressor.countFiles(archivePath)
        val lastFileIndex = (fileCount - 1).toDouble()
        var lastReportedProgress = 0
        check(tempArchiveFile.exists()) { "Downloaded temp archive does not exist." }
        tempExtractedFolder.mkdirs()
        compressor.extract(archivePath, tempExtractedFolder.getAbsolutePath())
            .flowOn(Dispatchers.Default)
            .collectIndexed { index, _ ->
                val completed = ((index / lastFileIndex) * 100).toInt()
                if (completed != lastReportedProgress && completed % 10 == 0) {
                    lastReportedProgress = completed
                    onProgress(JdkInstallProgress.Extract(completed))
                }
            }
        tempArchiveFile.delete()
        val newJdkName = "${distribution.name.lowercase()}-$jdkVersionString"
        val newJdkFolder = jdksRoot.nestedFile(newJdkName)
        // If extracted archive contains a single child folder, move that instead
        val moveTarget = tempExtractedFolder.listFiles().singleOrNull() ?: tempExtractedFolder
        return if (newJdkFolder.exists() && newJdkFolder.listFiles().isNotEmpty()) {
            JdkInstallResult.FileIOError(newJdkFolder, "JDK folder already exists and is not empty.")
        } else if (moveTarget.renameTo(newJdkFolder)) {
            JdkInstallResult.Success(checkNotNull(createInstallationDetails(newJdkFolder, pathEnv)))
        } else {
            JdkInstallResult.FileIOError(moveTarget, "Unable to move folder.")
        }
    }

    private suspend fun HttpClient.downloadPackage(
        downloadUrl: String,
        onProgress: (JdkInstallProgress) -> Unit,
        tempArchiveFile: File,
    ): HttpResponse = prepareGet(downloadUrl) {
        onProgress(JdkInstallProgress.Started(downloadUrl))
        var lastReportedProgress = 0
        onDownload { bytesSentTotal, contentLength ->
            val completed = ((bytesSentTotal.toDouble() / contentLength) * 100).toInt()
            if (completed != lastReportedProgress && completed % 10 == 0) {
                lastReportedProgress = completed
                onProgress(JdkInstallProgress.Download(completed))
            }
        }
    }.execute { response ->
        val body = response.bodyAsChannel()
        while (!body.isClosedForRead) {
            val packet = body.readRemaining(DOWNLOAD_BUFFER_SIZE)
            while (packet.isNotEmpty) {
                tempArchiveFile.appendBytes(packet.readBytes())
            }
        }
        response
    }

    private fun createInstallationDetails(file: File, pathEnv: String): InstallationDetails? {
        val fileName = file.getName() // JDK folders use the `zulu-18.0.1` format
        val distribution = try {
            JdkDistribution.valueOf(fileName.substringBefore('-').replaceFirstChar(Char::uppercase))
        } catch (e: IllegalArgumentException) {
            return null
        }
        val intellijManifestFilename = "${file.getParent()}$filePathSeparator.$fileName.intellij"
        return InstallationDetails(
            distribution = distribution,
            version = fileName.substringAfter('-'),
            intellijManifest = File(intellijManifestFilename).run {
                if (exists()) getAbsolutePath() else null
            },
            path = file.getAbsolutePath(),
            isActive = pathEnv.contains(fileName),
        )
    }

    private suspend fun availableTemurinVersions(
        http: HttpClient,
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
        http: HttpClient,
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
        val packageSuffix = if (Platform.osFamily == OsFamily.MACOSX) "" else "-jdk"
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

    private suspend fun availableZuluVersions(http: HttpClient, jdkVersion: String): Triple<String?, String?, String?> {
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

sealed class JdkInstallProgress {
    abstract val completed: Int

    data class Started(
        val downloadUrl: String,
        override val completed: Int = 0,
    ) : JdkInstallProgress()

    data class Download(override val completed: Int) : JdkInstallProgress()
    data class Extract(override val completed: Int) : JdkInstallProgress()
}

sealed class JdkInstallResult {
    data class Success(
        val installation: InstallationDetails,
    ) : JdkInstallResult()

    object NoMatchingVersion : JdkInstallResult()

    data class AlreadyInstalled(
        val installation: InstallationDetails,
    ) : JdkInstallResult()

    data class FileIOError(
        val file: File,
        val message: String,
    ) : JdkInstallResult()

    data class DownloadError(
        val url: String,
        val response: HttpResponse?,
        val cause: Throwable?,
    ) : JdkInstallResult()
}
