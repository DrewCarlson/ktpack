package ktpack.toolchains

import io.ktor.client.statement.*
import okio.Path


sealed class ToolchainInstallResult<out I : InstallDetails> {
    data class Success<I : InstallDetails>(
        val installation: I,
    ) : ToolchainInstallResult<I>()

    data object NoMatchingVersion : ToolchainInstallResult<Nothing>()

    data class AlreadyInstalled<I : InstallDetails>(
        val installation: I,
    ) : ToolchainInstallResult<I>()

    data class FileIOError<I : InstallDetails>(
        val file: Path,
        val message: String,
    ) : ToolchainInstallResult<I>()

    data class DownloadError<I : InstallDetails>(
        val url: String,
        val response: HttpResponse?,
        val cause: Throwable?,
    ) : ToolchainInstallResult<I>()
}

