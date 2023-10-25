package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.reset
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.*
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
import kotlin.system.exitProcess

class RunCommand : CliktCommand(
    help = "Compile and run binary packages.",
) {

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

        context.term.println(
            buildString {
                append(success("Compiling"))
                append(" ${packageConf.module.name}")
                append(" v${packageConf.module.version}")
                append(" (${moduleBuilder.srcFolder.parent})")
            },
        )
        val target = packageConf.module.validateTargetOrAlternative(context, userTarget) ?: return@runBlocking
        val result = terminal.loadingIndeterminate(
            animate = { text, duration ->
                bold(brightWhite(text)) + reset(white(" ${duration.inWholeSeconds}s"))
            },
        ) {
            moduleBuilder.buildBin(releaseMode, targetBin, target)
        }
        when (result) {
            is ArtifactResult.Success -> {
                if (context.debug) {
                    context.term.println(result.outputText)
                }
                context.term.println(
                    buildString {
                        append(success("Finished"))
                        if (releaseMode) {
                            append(" release [optimized] target(s)")
                        } else {
                            append(" dev [unoptimized + debuginfo] target(s)")
                        }
                        append(" in ${bold(white(result.compilationDuration.toString()))}s")
                    },
                )
                context.term.println("${success("Running")} '${result.artifactPath}'")
                try {
                    val (exitCode, duration) = measureSeconds {
                        runBuildArtifact(packageConf.module, target, result.artifactPath, result.dependencyArtifacts)
                    }
                    if (exitCode == 0) {
                        context.term.println(
                            "${success("Finished")} Program completed successfully in ${
                                bold(
                                    white(
                                        duration.toString(),
                                    ),
                                )
                            }s",
                        )
                    } else {
                        context.term.println(
                            "${failed("Failed")} Program terminated with code ($exitCode) in ${
                                bold(
                                    white(duration.toString()),
                                )
                            }s",
                        )
                    }
                } catch (e: IOException) {
                    context.term.println("${failed("Failed")} Program could not be started due to an IO error")
                    context.logError(e)
                }
            }

            is ArtifactResult.ProcessError -> {
                context.term.println("${failed("Failed")} Compilation process failed with exit code (${result.exitCode})")
                context.term.println(result.message.orEmpty())
            }

            is ArtifactResult.NoArtifactFound -> {
                context.term.println("${failed("Failed")} no binary to run")
            }

            is ArtifactResult.NoSourceFiles -> {
                context.term.println("${failed("Failed")} no source files")
            }
        }
    }

    private suspend fun CoroutineScope.runBuildArtifact(
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

    private suspend fun runJsBrowserArtifact(module: ModuleConf, artifactPath: String) = memScoped {
        val artifactName = artifactPath.substringAfterLast(DIRECTORY_SEPARATOR)
        runWebServer(
            httpPort = httpPort,
            data = HttpAccessHandlerData(
                moduleName = module.name,
                kotlinVersion = module.kotlinVersion ?: Ktpack.KOTLIN_VERSION,
                artifactPath = artifactPath,
                artifactName = artifactName
            ),
            onServerStarted = {
                context.term.println("${info("HTTP Server")} Available at http://localhost:$httpPort")
            }
        )
    }
}

