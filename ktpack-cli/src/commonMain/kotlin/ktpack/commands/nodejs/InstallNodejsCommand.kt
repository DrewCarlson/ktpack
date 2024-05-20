package ktpack.commands.nodejs

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import ktpack.CliContext
import ktpack.toolchain.ToolchainInstallProgress
import ktpack.toolchain.ToolchainInstallResult
import ktpack.util.*

class InstallNodejsCommand : CliktCommand(name = "install") {

    override fun help(context: Context): String {
        return context.theme.info("Install a new Nodejs version.")
    }

    private val context by requireObject<CliContext>()
    private val logger = Logger.withTag(InstallNodejsCommand::class.simpleName.orEmpty())

    private val version by argument()
        .help("The Nodejs version to install, can be a partial or full version. Example: 20 or 20.9.")

    private val path by option()
        .help("The root path to store the Nodejs installation.")
        .convert { Path(it) }
        .defaultLazy { Path(checkNotNull(context.config.nodejs.rootPath)) }
        .check({ "Nodejs root path must exist." }) { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs().exists()
        }

    override fun run() = runBlocking {
        // TODO: Resolve partial version to latest patch release (ex 1.8 -> 1.8.20)
        val existingInstalls = context.nodejsInstalls.discover(path)
        val matchedInstalls = existingInstalls.filter { it.version == version }
        if (matchedInstalls.isNotEmpty()) {
            logger.i("${warn("Warning")} Existing installation found for $version, nothing to do")
            return@runBlocking
        }

        logger.i("${info("Nodejs")} Downloading Nodejs ${info(version)}")
        val (installResult, duration) = measureSeconds {
            context.nodejsInstalls.findAndInstall(path, version) { state ->
                when (state) {
                    is ToolchainInstallProgress.Started -> logger.i("${info("Downloading")} ${state.downloadUrl}")
                    is ToolchainInstallProgress.Download -> logger.i("${info("Downloading")} ${state.completed}%")
                    is ToolchainInstallProgress.Extract -> logger.i("${info("Extracting")} ${state.completed}%")
                }
            }
        }

        when (installResult) {
            is ToolchainInstallResult.Success -> {
                val install = installResult.installation
                logger.i("${success("Success")} Nodejs ${info(install.version)} was installed to '${install.path}' in ${duration}s")
            }

            is ToolchainInstallResult.AlreadyInstalled -> {
                logger.i("${failed("Downloading")} The selected Nodejs version is already installed.")
            }

            is ToolchainInstallResult.DownloadError -> {
                logger.i("${failed("Downloading")} Error while downloading Nodejs")
                if (installResult.cause != null) {
                    logger.i("${failed("Downloading")} ${installResult.cause!!.stackTraceToString()}")
                } else if (installResult.response != null) {
                    logger.i("${failed("Downloading")} Server responded with ${installResult.response!!.status}")
                }
            }

            is ToolchainInstallResult.FileIOError -> {
                logger.i("${failed("Extracting")} ${installResult.message} ${installResult.file}")
            }

            ToolchainInstallResult.NoMatchingVersion -> {
                logger.i("${failed("Failed")} No releases found matching $version")
            }
        }
    }
}
