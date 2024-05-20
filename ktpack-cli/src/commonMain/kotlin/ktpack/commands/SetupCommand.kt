package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.prompt
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import ktpack.CliContext
import ktpack.toolchain.jdk.JdkInstallDetails
import ktpack.toolchain.kotlin.KotlinInstallDetails
import ktpack.toolchain.kotlin.KotlincInstalls
import ktpack.toolchain.nodejs.NodejsInstallDetails
import ktpack.toolchain.ToolchainInstallProgress
import ktpack.toolchain.ToolchainInstallResult
import ktpack.util.*

class SetupCommand : CliktCommand(name = "setup") {

    override fun help(context: Context): String {
        return context.theme.info("Setup your environment to build and run packages.")
    }

    private val logger = Logger.withTag(SetupCommand::class.simpleName.orEmpty())
    private val context by requireObject<CliContext>()

    private val autoInstall by option("--install", "-i")
        .help("Automatically install all missing tools.")
        .flag()

    override fun run(): Unit = runBlocking {
        logger.i("${info("Setup")} Scanning your environment for available tools.")
        val isKotlinSetup = setupKotlin()
        val isJdkSetup = setupJdk()
        val isNodeSetup = setupNodejs()
        if (isKotlinSetup && isJdkSetup && isNodeSetup) {
            logger.i("${success("Setup")} Your environment is ready for Kotlin!")
            return@runBlocking
        } else {
            val message = buildString {
                append(failed("Setup"))
                append(" Some tools are missing from your environment: ")
                append(
                    listOfNotNull(
                        "Kotlin".takeUnless { isKotlinSetup },
                        "JDK".takeUnless { isJdkSetup },
                        "Node".takeUnless { isNodeSetup },
                    ).joinToString()
                )
            }
            logger.i(message)
            exitProcess(1)
        }
    }

    private suspend fun setupNodejs(): Boolean {
        logger.i("${info("Setup")} Checking for Nodejs")
        val root = Path(context.config.nodejs.rootPath)
        val version = context.config.nodejs.version
        val existingNodejs: NodejsInstallDetails? = context.nodejsInstalls.getDefaultNodejs()

        if (existingNodejs == null) {
            logger.i("${warn("Setup")} No default Nodejs installation found")
            val answer = if (autoInstall) {
                "y"
            } else {
                context.term.prompt(
                    "${info("Setup")} Would you like to install Nodejs v${info(version)}?",
                    default = "y",
                )
            }

            if (answer == null || answer.equals("n", ignoreCase = true)) {
                logger.i("${info("Setup")} Skipping Nodejs installation.")
                return false
            } else {
                val result = context.nodejsInstalls.findAndInstall(root, version, progressLog)
                when (result) {
                    is ToolchainInstallResult.Success -> {
                        logger.i("${success("Setup")} Nodejs installation completed!")
                    }
                    is ToolchainInstallResult.AlreadyInstalled -> {
                        logger.i("${success("Setup")} Nodejs installation completed!")
                        return false
                    }
                    is ToolchainInstallResult.DownloadError,
                    is ToolchainInstallResult.FileIOError,
                    ToolchainInstallResult.NoMatchingVersion -> {
                        logger.i("${failed("Setup")} Failed to download Nodejs.")
                        return false
                    }
                }
            }
        } else {
            logger.i("${success("Setup")} Default Nodejs installation found")
        }
        return true
    }

    private suspend fun setupKotlin(): Boolean {
        logger.i("${info("Setup")} Checking for Kotlin compilers")
        val root = Path(context.config.kotlin.rootPath)
        val version = context.config.kotlin.version
        val existingKotlinJvm: KotlinInstallDetails? =
            context.kotlinInstalls.getDefaultKotlin(KotlincInstalls.CompilerType.JVM)
        if (existingKotlinJvm == null) {
            logger.i("${warn("Setup")} No default Kotlin JVM installation found")
            val answer = if (autoInstall) {
                "y"
            } else {
                context.term.prompt(
                    "${info("Setup")} Would you like to install Kotlin JVM v${info(version)}?",
                    default = "y",
                )
            }
            if (answer == null || answer.equals("n", ignoreCase = true)) {
                logger.i("${info("Setup")} Skipping Kotlin JVM installation.")
                return false
            } else {
                val result = context.kotlinInstalls.findAndInstallKotlin(
                    root,
                    version,
                    KotlincInstalls.CompilerType.JVM,
                    progressLog,
                )
                when (result) {
                    is ToolchainInstallResult.Success -> {
                        logger.i("${success("Setup")} Kotlin installation completed!")
                    }
                    is ToolchainInstallResult.AlreadyInstalled -> {
                        logger.i("${success("Setup")} Kotlin installation completed!")
                        return false
                    }
                    is ToolchainInstallResult.DownloadError,
                    is ToolchainInstallResult.FileIOError,
                    ToolchainInstallResult.NoMatchingVersion -> {
                        logger.i("${failed("Setup")} Failed to download Kotlin JVM.")
                        return false
                    }
                }
            }
        } else {
            logger.i("${success("Setup")} Default Kotlin JVM installation found")
        }
        val existingKotlinNative: KotlinInstallDetails? =
            context.kotlinInstalls.getDefaultKotlin(KotlincInstalls.CompilerType.NATIVE)
        if (existingKotlinNative == null) {
            logger.i("${warn("Setup")} No default Kotlin Native installation found")
            val answer = if (autoInstall) {
                "y"
            } else {
                context.term.prompt(
                    "${info("Setup")} Would you like to install Kotlin Native v${info(version)}?",
                    default = "y",
                )
            }
            if (answer == null || answer.equals("n", ignoreCase = true)) {
                logger.i("${info("Setup")} Skipping Kotlin JVM installation.")
                return false
            } else {
                val result = context.kotlinInstalls.findAndInstallKotlin(
                    root,
                    version,
                    KotlincInstalls.CompilerType.NATIVE,
                    progressLog,
                )
                when (result) {
                    is ToolchainInstallResult.Success -> {
                        logger.i("${success("Setup")} Kotlin Native installation completed!")
                    }
                    is ToolchainInstallResult.AlreadyInstalled -> {
                        logger.i("${success("Setup")} Kotlin Native installation completed!")
                        return false
                    }
                    is ToolchainInstallResult.DownloadError,
                    is ToolchainInstallResult.FileIOError,
                    ToolchainInstallResult.NoMatchingVersion -> {
                        logger.i("${failed("Setup")} Failed to download Kotlin Native.")
                        return false
                    }
                }
            }
        } else {
            logger.i("${success("Setup")} Default Kotlin Native installation found")
        }
        return true
    }

    private suspend fun setupJdk(): Boolean {
        logger.i("${info("Setup")} Checking for JDK")
        val existingJdk: JdkInstallDetails? = context.jdkInstalls.getDefaultJdk()
        if (existingJdk == null) {
            val distribution = context.config.jdk.distribution
            val version = context.config.jdk.version
            logger.i("${warn("Setup")} No default JDK installation found")
            val answer = if (autoInstall == true) {
                "y"
            } else {
                context.term.prompt(
                    "${info("Setup")} Would you like to install ${info(distribution.name)} JDK v${info(version)}?",
                    default = "y",
                )
            }
            if (answer == null || answer.equals("n", ignoreCase = true)) {
                logger.i("${info("Setup")} Skipping JDK installation.")
                return false
            } else {
                val root = Path(context.config.jdk.rootPath)
                val result = context.jdkInstalls.findAndInstallJdk(root, version, distribution, progressLog)
                when (result) {
                    is ToolchainInstallResult.Success -> {
                        logger.i("${success("Setup")} JDK installation completed!")
                    }
                    is ToolchainInstallResult.AlreadyInstalled -> {
                        logger.i("${success("Setup")} JDK installation completed!")
                        return false
                    }
                    is ToolchainInstallResult.DownloadError,
                    is ToolchainInstallResult.FileIOError,
                    ToolchainInstallResult.NoMatchingVersion -> {
                        logger.i("${failed("Setup")} Failed to download JDK.")
                        return false
                    }
                }
            }
        } else {
            logger.i("${success("Setup")} Default JDK version found ${existingJdk.version} ${existingJdk.distribution}")
        }
        return true
    }

    private val progressLog: (ToolchainInstallProgress) -> Unit = { state ->
        when (state) {
            is ToolchainInstallProgress.Started -> logger.i("${info("Downloading")} ${state.downloadUrl}")
            is ToolchainInstallProgress.Download -> logger.i("${info("Downloading")} ${state.completed}%")
            is ToolchainInstallProgress.Extract -> logger.i("${info("Extracting")} ${state.completed}%")
        }
    }
}
