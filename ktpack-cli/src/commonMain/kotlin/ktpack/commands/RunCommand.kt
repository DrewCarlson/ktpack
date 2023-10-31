package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.reset
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ksubprocess.*
import ktpack.*
import ktpack.compilation.ArtifactResult
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf
import ktpack.util.*
import mongoose.*
import okio.Path.Companion.DIRECTORY_SEPARATOR
import okio.Path.Companion.toPath

class RunCommand : CliktCommand(
    help = "Compile and run binary packages.",
) {

    private val logger = Logger.withTag(RunCommand::class.simpleName.orEmpty())
    private val context by requireObject<CliContext>()

    private val releaseMode by option("--release")
        .help("Run binary in release mode, with optimizations")
        .flag(default = false)

    private val targetBin by option("--bin")
        .help("Run the specified binary")

    private val userTarget by option("--target", "-t")
        .help("The target platform to build for.")
        .enum<KotlinTarget>()

    private val httpPort by option("--port", "-p")
        .help("The HTTP Server port to use when serving the js_browser target.")
        .int()
        .default(9543)

    override fun run(): Unit = runBlocking {
        val packageConf = context.loadKtpackConf()
        val moduleBuilder = ModuleBuilder(packageConf.module, context, workingDirectory)
        val targetBin = targetBin ?: packageConf.module.name

        logger.i {
            val name = packageConf.module.name
            val version = packageConf.module.version
            val modulePath = moduleBuilder.modulePath
            "${success("Compiling")} $name v$version ($modulePath)"
        }
        val target = packageConf.module.validateTargetOrAlternative(context, userTarget) ?: return@runBlocking
        /*val result = terminal.loadingIndeterminate(
            animate = { text, duration ->
                bold(brightWhite(text)) + reset(white(" ${duration.inWholeSeconds}s"))
            },
        ) {
            moduleBuilder.buildBin(releaseMode, targetBin, target)
        }*/
        val result = moduleBuilder.buildBin(releaseMode, targetBin, target)
        when (result) {
            is ArtifactResult.Success -> {
                logger.d { result.outputText }
                logger.i {
                    val duration = result.compilationDuration.toString()
                    val modeDetails = if (releaseMode) {
                        "release [optimized] target(s)"
                    } else {
                        "dev [unoptimized + debuginfo] target(s)"
                    }
                    "${success("Finished")} $modeDetails in ${bold(white(duration))}s"
                }
                context.term.println("${success("Running")} '${result.artifactPath}'")
                try {
                    val (exitCode, duration) = measureSeconds {
                        runBuildArtifact(packageConf.module, target, result.artifactPath, result.dependencyArtifacts)
                    }
                    val durationString = bold(white(duration.toString()))
                    if (exitCode == 0) {
                        logger.i {
                            "${success("Finished")} Program completed successfully in ${durationString}s"
                        }
                    } else {
                        logger.i {
                            "${failed("Failed")} Program terminated with code ($exitCode) in ${durationString}s"
                        }
                    }
                } catch (e: IOException) {
                    logger.i {
                        "${failed("Failed")} Program could not be started due to an IO error"
                    }
                    logger.i { e.message.orEmpty() }
                    logger.e { e.stackTraceToString() }
                }
            }

            is ArtifactResult.ProcessError -> {
                logger.i { "${failed("Failed")} Compilation process failed with exit code (${result.exitCode})" }
                logger.i { result.message.orEmpty() }
            }

            is ArtifactResult.NoArtifactFound -> {
                logger.i { "${failed("Failed")} no binary to run" }
            }

            is ArtifactResult.NoSourceFiles -> {
                logger.i { "${failed("Failed")} no source files" }
            }
        }
    }

    private suspend fun runBuildArtifact(
        module: ModuleConf,
        target: KotlinTarget,
        artifactPath: String,
        dependencyArtifacts: List<String>,
    ): Int {
        if (target == KotlinTarget.JS_BROWSER) {
            runJsBrowserArtifact(module, artifactPath)
            return 1
        }
        val runProcess = Process {
            when (target) {
                KotlinTarget.JS_BROWSER -> error("Unsupported run target: $target")
                KotlinTarget.JVM -> {
                    val jdkInstallation = context.jdkInstalls.getDefaultJdk()
                    if (jdkInstallation == null) {
                        context.term.println("${failed("Failed")} Could not find JDK installation.")
                        exitProcess(1)
                    }
                    arg(pathFrom(jdkInstallation.path, "bin", "java").name)
                    args("-classpath", (dependencyArtifacts + artifactPath).joinToString(CPSEP))
                    arg("MainKt") // TODO: get main class from artifact
                }

                KotlinTarget.JS_NODE -> {
                    arg("C:\\Users\\drewc\\.gradle\\nodejs\\node-v16.13.0-win-x64\\node.exe")
                    arg(artifactPath)
                }

                else -> {
                    // native targets
                    arg(artifactPath)
                }
            }
        }

        val stdoutPrefix = bold(brightBlue("[out] "))
        val stderrPrefix = bold(brightRed("[err] "))
        return Dispatchers.Default {
            merge(
                runProcess.stdoutLines.map { stdoutPrefix + reset(it) },
                runProcess.stderrLines.map { stderrPrefix + reset(it) },
            ).collect { line ->
                context.term.println(line)
            }

            runProcess.waitFor()
        }
    }

    private suspend fun runJsBrowserArtifact(module: ModuleConf, artifactPath: String) {
        runWebServer(
            httpPort = httpPort,
            data = HttpAccessHandlerData(
                moduleName = module.name,
                kotlinVersion = module.kotlinVersion ?: Ktpack.KOTLIN_VERSION,
                artifactPath = artifactPath,
                artifactName = artifactPath.toPath().name
            ),
            onServerStarted = {
                context.term.println("${info("HTTP Server")} Available at http://localhost:$httpPort")
            },
        )
    }
}

