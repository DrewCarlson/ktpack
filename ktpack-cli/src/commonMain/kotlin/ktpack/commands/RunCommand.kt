package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.reset
import io.ktor.http.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ksubprocess.*
import ktpack.*
import ktpack.compilation.ArtifactResult
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
import ktpack.manifest.ModuleToml
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
        val manifest = context.loadManifestToml()
        val moduleBuilder = ModuleBuilder(manifest, context, workingDirectory)
        val targetBin = targetBin ?: manifest.module.name

        logger.i {
            val name = manifest.module.name
            val version = manifest.module.version
            val modulePath = moduleBuilder.modulePath
            "${success("Compiling")} $name v$version ($modulePath)"
        }
        val target = manifest.module.validateTargetOrAlternative(context, userTarget) ?: return@runBlocking
        val result = terminal.loadingIndeterminate {
            moduleBuilder.buildBin(releaseMode, targetBin, target)
        }
        when (result) {
            is ArtifactResult.Success -> {
                logger.d { "Compiler output:\n ${result.outputText}" }
                logger.i {
                    val duration = result.compilationDuration.toString()
                    val modeDetails = if (releaseMode) {
                        "release [optimized] target(s)"
                    } else {
                        "dev [unoptimized + debuginfo] target(s)"
                    }
                    "${success("Finished")} $modeDetails in ${bold(white(duration))}s"
                }
                logger.i("${success("Running")} '${result.artifactPath}'")
                try {
                    val (exitCode, duration) = measureSeconds {
                        runBuildArtifact(manifest.module, target, result.artifactPath, result.dependencyArtifacts)
                    }
                    val durationString = bold(white(duration.toString()))
                    if (exitCode == 0) {
                        logger.i("${success("Finished")} Program completed successfully in ${durationString}s")
                    } else {
                        logger.i("${failed("Failed")} Program terminated with code ($exitCode) in ${durationString}s")
                    }
                } catch (e: IOException) {
                    logger.i("${failed("Failed")} Program could not be started due to an IO error")
                    throw e
                }
            }

            is ArtifactResult.ProcessError -> {
                logger.i("${failed("Failed")} Compilation process failed with exit code (${result.exitCode})")
                logger.i(result.message.orEmpty())
            }

            is ArtifactResult.NoArtifactFound -> {
                logger.i("${failed("Failed")} no binary to run")
            }

            is ArtifactResult.NoSourceFiles -> {
                logger.i("${failed("Failed")} no source files")
            }
        }
    }

    private suspend fun runBuildArtifact(
        module: ModuleToml,
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
                        logger.i("${failed("Failed")} Could not find JDK installation.")
                        exitProcess(1)
                    }
                    arg(pathFrom(jdkInstallation.path, "bin", "java").name)
                    args("-classpath", (dependencyArtifacts + artifactPath).joinToString(CPSEP))
                    arg("MainKt") // TODO: get main class from artifact
                }

                KotlinTarget.JS_NODE -> {
                    val nodeJs = context.nodejsInstalls.findNodejsExe(context.config.nodejs.version)
                    if (nodeJs == null) {
                        logger.i("${failed("Failed")} Could not find Nodejs installation.")
                        exitProcess(1)
                    }
                    arg(nodeJs.toString())
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

    private suspend fun runJsBrowserArtifact(module: ModuleToml, artifactPath: String) {
        runWebServer(
            httpPort,
            onServerStarted = {
                logger.i("${info("HTTP Server")} Available at http://localhost:$httpPort")
            },
        ) {
            indexRoute {
                contentType(ContentType.Text.Html)
                respondBody(
                    DEFAULT_HTML.format(
                        module.name,
                        module.kotlinVersion ?: Ktpack.KOTLIN_VERSION,
                        artifactPath.substringAfterLast(DIRECTORY_SEPARATOR),
                    ),
                )
            }
            route("/${artifactPath.toPath().name}") { respondFile(artifactPath) }
            route("/*") { respondDirectory(".") }
        }
    }
}

// TODO: Extract and link kotlin.js from the classpath stdlib-js jar
private val DEFAULT_HTML =
    """|<!DOCTYPE html>
       |<html>
       |<head>
       |    <meta charset=UTF-8>
       |    <title>{}</title>
       |    <script defer="defer" src="https://cdnjs.cloudflare.com/ajax/libs/kotlin/{}/kotlin.min.js" type="application/javascript"></script>
       |    <script defer="defer" src="{}" type="application/javascript"></script>
       |</head>
       |<body></body>
       |</html>
    """.trimMargin()
