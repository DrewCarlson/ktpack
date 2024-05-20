package ktpack.toolchain

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import ktpack.util.*

abstract class ToolchainInstaller<I : InstallDetails>(
    protected val http: HttpClient,
) {
    protected val logger = Logger.withTag(this::class.simpleName.orEmpty())
    protected val pathEnv = getEnv("PATH").orEmpty()
    protected val packageExtension by lazy {
        when (Platform.osFamily) {
            OsFamily.WINDOWS -> ".zip"
            OsFamily.LINUX, OsFamily.MACOSX -> ".tar.gz"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
    }

    abstract fun discover(rootPath: Path): List<I>

    private suspend fun downloadPackage(
        downloadUrl: String,
        onProgress: (ToolchainInstallProgress) -> Unit,
        tempArchivePath: Path,
    ): HttpResponse = http.prepareGet(downloadUrl) {
        onProgress(ToolchainInstallProgress.Started(downloadUrl))
        var lastReportedProgress = 0
        onDownload { bytesSentTotal, contentLength ->
            // todo: content length is nullable?
            val completed = ((bytesSentTotal.toDouble() / (contentLength ?: 1)) * 100).toInt()
            if (completed != lastReportedProgress && completed % 10 == 0) {
                lastReportedProgress = completed
                onProgress(ToolchainInstallProgress.Download(completed))
            }
        }
    }.downloadInto(tempArchivePath)

    protected suspend fun downloadAndExtract(
        downloadUrl: String,
        onProgress: (ToolchainInstallProgress) -> Unit,
    ): Pair<ToolchainInstallResult<I>?, Path> {
        val archiveName = downloadUrl.substringAfterLast('/')
        val tempExtractPath = Path(SystemTemporaryDirectory, archiveName.substringBeforeLast(packageExtension))
        val tempArchivePath = Path(SystemTemporaryDirectory, archiveName)
        if (!tempArchivePath.createNewFile()) {
            logger.e { "Failed to create temp file: $tempArchivePath" }
            return Pair(
                ToolchainInstallResult.FileIOError(tempArchivePath, "Failed to create temp file"),
                tempExtractPath,
            )
        }

        val response = try {
            downloadPackage(downloadUrl, onProgress, tempArchivePath)
        } catch (e: Throwable) {
            logger.e(e) { "Failed to download package from $downloadUrl into $tempArchivePath" }
            return Pair(
                ToolchainInstallResult.DownloadError(downloadUrl, null, e),
                tempExtractPath,
            )
        }

        if (!response.status.isSuccess()) {
            logger.e { "Download request failed with response status ${response.status}" }
            return Pair(
                ToolchainInstallResult.DownloadError(downloadUrl, response, null),
                tempExtractPath,
            )
        }

        val compressor = if (Platform.osFamily == OsFamily.WINDOWS) Zip else Tar
        val archivePathString = tempArchivePath.toString()
        val fileCount = compressor.countFiles(archivePathString)
        val lastFileIndex = (fileCount - 1).toDouble()
        var lastReportedProgress = 0
        if (!tempExtractPath.mkdirs().exists()) {
            logger.e("Failed to create temp archive extract directory: $tempExtractPath")
            return Pair(
                ToolchainInstallResult.FileIOError(tempExtractPath, "Failed to temp directory."),
                tempArchivePath,
            )
        }
        compressor.extract(archivePathString, tempExtractPath.toString())
            .flowOn(Dispatchers.Default)
            .collectIndexed { index, _ ->
                val completed = ((index / lastFileIndex) * 100).toInt()
                if (completed != lastReportedProgress && completed % 10 == 0) {
                    lastReportedProgress = completed
                    onProgress(ToolchainInstallProgress.Extract(completed))
                }
            }
        tempArchivePath.delete()
        return null to tempExtractPath
    }

    protected fun moveExtractedFiles(
        installDetails: I,
        extractPath: Path,
        newFolderPath: Path,
    ): ToolchainInstallResult<I> {
        // If extracted archive contains a single child folder, move that instead
        val target = extractPath.list().singleOrNull() ?: extractPath
        return if (newFolderPath.exists() && newFolderPath.list().isNotEmpty()) {
            ToolchainInstallResult.FileIOError(newFolderPath, "Folder already exists")
        } else if (target.renameTo(newFolderPath)) {
            ToolchainInstallResult.Success(installDetails)
        } else {
            ToolchainInstallResult.FileIOError(target, "Unable to move folder.")
        }
    }
}
