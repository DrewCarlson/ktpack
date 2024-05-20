package ktpack.commands.jdk

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import ktpack.CliContext
import ktpack.toolchain.jdk.JdkDistribution
import ktpack.toolchain.ToolchainInstallProgress
import ktpack.toolchain.ToolchainInstallResult
import ktpack.util.*

class InstallJdkCommand : CliktCommand(name = "install") {

    override fun help(context: Context): String {
        return context.theme.info("Install a new JDK version.")
    }

    private val context by requireObject<CliContext>()

    private val version by argument()
        .help("The JDK version to install, can be a partial or full version. Example: 8 or 8.0.342.")
        .check({ "JDK version must be 8 or higher (11+ is recommended)." }) { version ->
            version.substringBefore('.').toInt() >= 8
        }

    private val distribution by option("--distribution", "-d")
        .help("The JDK distribution to install.")
        .enum<JdkDistribution>()
        .default(JdkDistribution.Zulu)

    private val path by option()
        .help("The root path to store the JDK installation.")
        .convert { Path(it) }
        .defaultLazy { Path(checkNotNull(context.config.jdk.rootPath)) }
        .check({ "JDK root path must exist." }) { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs().exists()
        }

    override fun run() = runBlocking {
        val existingInstalls = context.jdkInstalls.discover(path)
        val matchedInstall = existingInstalls.firstOrNull { it.distribution == distribution && it.version == version }
        if (matchedInstall != null) {
            context.term.println("${warn("Warning")} Existing installation found at ${matchedInstall.path}, nothing to do")
            return@runBlocking
        }

        context.term.println("${info("JDKs")} Fetching available $distribution JDK versions")

        val (installResult, duration) = measureSeconds {
            context.jdkInstalls.findAndInstallJdk(path, version, distribution) { state ->
                when (state) {
                    is ToolchainInstallProgress.Started -> context.term.println("${info("Downloading")} ${state.downloadUrl}")
                    is ToolchainInstallProgress.Download -> context.term.println("${info("Downloading")} ${state.completed}%")
                    is ToolchainInstallProgress.Extract -> context.term.println("${info("Extracting")} ${state.completed}%")
                }
            }
        }

        when (installResult) {
            is ToolchainInstallResult.Success -> {
                val installation = installResult.installation
                context.term.println(
                    buildString {
                        append(success("Success"))
                        append(" JDK ")
                        append(info(distribution.name))
                        append(' ')
                        append(info(installation.version))
                        append(" was installed to '")
                        append(installation.path)
                        append("' in ")
                        append(duration)
                        append('s')
                    },
                )
            }

            is ToolchainInstallResult.AlreadyInstalled -> {
                context.term.println("${failed("Downloading")} The selected JDK version is already installed.")
            }

            is ToolchainInstallResult.DownloadError -> {
                context.term.println("${failed("Downloading")} Error while downloading JDK")
                if (installResult.cause != null) {
                    context.term.println("${failed("Downloading")} ${installResult.cause!!.stackTraceToString()}")
                } else if (installResult.response != null) {
                    context.term.println("${failed("Downloading")} Server responded with ${installResult.response!!.status}")
                }
            }

            is ToolchainInstallResult.FileIOError -> {
                context.term.println("${failed("Extracting")} ${installResult.message} ${installResult.file}")
            }

            ToolchainInstallResult.NoMatchingVersion -> {
                context.term.println("${failed("Failed")} No packages found matching ${distribution.name}@$version")
            }
        }
    }
}
