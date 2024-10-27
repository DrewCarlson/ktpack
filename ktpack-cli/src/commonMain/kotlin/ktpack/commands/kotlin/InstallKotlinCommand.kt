package ktpack.commands.kotlin

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import ktpack.CliContext
import ktpack.toolchain.kotlin.KotlinInstallDetails
import ktpack.toolchain.ToolchainInstallProgress
import ktpack.toolchain.ToolchainInstallResult
import ktpack.toolchain.kotlin.KotlinInstallDetails.CompilerType
import ktpack.util.*

class InstallKotlinCommand : CliktCommand(name = "install") {
    override fun help(context: Context): String {
        return context.theme.info("Install a new Kotlin Compiler version.")
    }

    private val context by requireObject<CliContext>()
    private val logger = Logger.forClass<InstallKotlinCommand>()

    private val version by argument()
        .help("The Kotlin version to install, can be a partial or full version. Example: 1.8 or 1.8.20.")
        .check(
            validator = { it.split('.').size >= 2 },
            lazyMessage = {
                "You must specify at least a major AND minor version for Kotlin.  Example: 1.8 or 1.8.20"
            },
        )

    private val path by option()
        .help("The root path to store the Kotlin installation.")
        .convert { Path(it) }
        .defaultLazy { Path(checkNotNull(context.config.kotlin.rootPath)) }
        .check({ "Kotlin root path must exist." }) { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs().exists()
        }

    private val compilerType by option("--compiler", "-c")
        .enum<CompilerType> { it.name.lowercase() }

    override fun run() = runBlocking {
        // TODO: Resolve partial version to latest patch release (ex 1.8 -> 1.8.20)
        val existingInstalls = context.kotlinInstalls.discover(path)
        val matchedInstalls = existingInstalls.filter { it.version == version }
        if (matchedInstalls.size == 2 || existingInstalls.any { it.type == compilerType }) {
            logger.i("${warn("Warning")} Existing installation found for ${compilerType?.let(::listOf) ?: CompilerType.entries}, nothing to do")
            return@runBlocking
        }

        val newInstalls = mutableListOf<Pair<KotlinInstallDetails, Double>>()
        val compilerType = compilerType
        if (compilerType == null) {
            if (matchedInstalls.none { it.type == CompilerType.JVM }) {
                downloadAndSetupKotlin(CompilerType.JVM)?.run(newInstalls::add)
            }
            if (matchedInstalls.none { it.type == CompilerType.NATIVE }) {
                downloadAndSetupKotlin(CompilerType.NATIVE)?.run(newInstalls::add)
            }
        } else {
            downloadAndSetupKotlin(compilerType)?.run(newInstalls::add)
        }
        newInstalls.forEach { (install, duration) ->
            logger.i("${success("Success")} Kotlin ${install.type} compiler ${info(install.version)} was installed to '${install.path}' in ${duration}s")
        }
    }

    private suspend fun downloadAndSetupKotlin(
        compilerType: CompilerType
    ): Pair<KotlinInstallDetails, Double>? {
        logger.i("${info("Kotlin")} Downloading compiler for $compilerType")
        val (installResult, duration) = measureSeconds {
            context.kotlinInstalls.findAndInstallKotlin(path, version, compilerType) { state ->
                when (state) {
                    is ToolchainInstallProgress.Started -> logger.i("${info("Downloading")} ${state.downloadUrl}")
                    is ToolchainInstallProgress.Download -> logger.i("${info("Downloading")} ${state.completed}%")
                    is ToolchainInstallProgress.Extract -> logger.i("${info("Extracting")} ${state.completed}%")
                }
            }
        }

        return when (installResult) {
            is ToolchainInstallResult.Success -> installResult.installation to duration

            is ToolchainInstallResult.AlreadyInstalled -> {
                logger.i("${failed("Downloading")} The selected Kotlin version is already installed.")
                null
            }

            is ToolchainInstallResult.DownloadError -> {
                logger.i("${failed("Downloading")} Error while downloading Kotlin")
                if (installResult.cause != null) {
                    logger.i("${failed("Downloading")} ${installResult.cause!!.stackTraceToString()}")
                } else if (installResult.response != null) {
                    logger.i("${failed("Downloading")} Server responded with ${installResult.response!!.status}")
                }
                null
            }

            is ToolchainInstallResult.FileIOError -> {
                logger.i("${failed("Extracting")} ${installResult.message} ${installResult.file}")
                null
            }

            ToolchainInstallResult.NoMatchingVersion -> {
                logger.i("${failed("Failed")} No releases found matching $version")
                null
            }
        }
    }
}
