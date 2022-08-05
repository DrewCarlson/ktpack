package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ksubprocess.*
import ktfio.File
import ktfio.filePathSeparator
import ktpack.*
import ktpack.compilation.ArtifactResult
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf
import ktpack.util.*
import mongoose.*
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
        val manifest = context.loadManifest()
        val moduleBuilder = ModuleBuilder(manifest.module, context, workingDirectory)
        val targetBin = targetBin ?: manifest.module.name

        context.term.println(
            buildString {
                append(success("Compiling"))
                append(" ${manifest.module.name}")
                append(" v${manifest.module.version}")
                append(" (${moduleBuilder.srcFolder.getParent()})")
            }
        )
        val target = manifest.module.validateTargetOrAlternative(context, userTarget) ?: return@runBlocking
        when (val result = moduleBuilder.buildBin(releaseMode, targetBin, target)) {
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
                        append(" in ${result.compilationDuration}s")
                    }
                )
                context.term.println("${success("Running")} '${result.artifactPath}'")
                try {
                    val (exitCode, duration) = measureSeconds {
                        runBuildArtifact(manifest.module, target, result.artifactPath, result.dependencyArtifacts)
                    }
                    if (exitCode == 0) {
                        context.term.println("${success("Finished")} Program completed successfully in ${duration}s")
                    } else {
                        context.term.println("${failed("Failed")} Program terminated with code ($exitCode) in ${duration}s")
                    }
                } catch (e: IOException) {
                    context.term.println("${failed("Failed")} Program could not be started due to an IO error")
                    if (context.stacktrace) {
                        context.term.println((e.cause ?: e).stackTraceToString())
                    }
                }
            }

            is ArtifactResult.ProcessError -> {
                context.term.println("${failed("Failed")} Compilation process failed with exit code (${result.exitCode})")
                context.term.println(result.message.orEmpty())
            }

            is ArtifactResult.NoArtifactFound -> {
                context.term.println("${failed("Failed")} no binary to run")
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
        return Process {
            when (target) {
                KotlinTarget.JS_BROWSER -> error("Unsupported run target: $target")
                KotlinTarget.JVM -> {
                    val jdkInstallation = context.jdkInstalls.getDefaultJdk()
                    if (jdkInstallation == null) {
                        context.term.println("${failed("Failed")} Could not find JDK installation.")
                        exitProcess(1)
                    }
                    arg(File(jdkInstallation.path, "bin", "java").getAbsolutePath())
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
        }.run {
            Dispatchers.Default {
                val writeLock = Mutex()
                merge(stdoutLines, stderrLines).collect { line ->
                    writeLock.withLock { context.term.println(line) }
                }
                waitFor()
            }
        }
    }

    private suspend fun runJsBrowserArtifact(module: ModuleConf, artifactPath: String) = memScoped {
        val artifactName = artifactPath.substringAfterLast(filePathSeparator)
        val manager = alloc<mg_mgr>()
        val arg = StableRef.create(HttpAccessHandlerData(module, artifactPath, artifactName))
        mg_mgr_init(manager.ptr)
        mg_http_listen(manager.ptr, "http://0.0.0.0:$httpPort", httpFunc, arg.asCPointer())
        context.term.println("${info("HTTP Server")} Available at http://localhost:$httpPort")

        defer { mg_mgr_free(manager.ptr) }
        while (currentCoroutineContext().isActive) {
            mg_mgr_poll(manager.ptr, mg_millis().convert())
            yield()
        }
    }
}

private data class HttpAccessHandlerData(
    val module: ModuleConf,
    val artifactPath: String,
    val artifactName: String,
)

private val httpFunc: mg_event_handler_t = staticCFunction { con, ev, evData, fnData ->
    if (ev.toUInt() == MG_EV_HTTP_MSG) {
        val (module, artifactPath, artifactName) = checkNotNull(fnData?.asStableRef<HttpAccessHandlerData>()).get()
        memScoped {
            val hm = checkNotNull(evData?.reinterpret<mg_http_message>()).pointed
            if (mg_http_match_uri(hm.ptr, "/") || mg_http_match_uri(hm.ptr, "/index.html")) {
                mg_http_reply(
                    con,
                    200,
                    "Content-Type: text/html\r\n",
                    DEFAULT_HTML,
                    module.name,
                    module.kotlinVersion,
                    artifactName
                )
            } else if (mg_http_match_uri(hm.ptr, "/$artifactName")) {
                val opts = alloc<mg_http_serve_opts> {
                    mime_types = "js=application/javascript".cstr.ptr
                }
                mg_http_serve_file(con, hm.ptr, artifactPath, opts.ptr)
            } else {
                val opts = alloc<mg_http_serve_opts> {
                    root_dir = ".".cstr.ptr
                }
                mg_http_serve_dir(con, evData?.reinterpret(), opts.ptr)
            }
        }
    }
}

private val DEFAULT_HTML =
    """|<!DOCTYPE html>
       |<html>
       |<head>
       |    <meta charset=UTF-8>
       |    <title>%s</title>
       |    <script defer="defer" src="https://cdnjs.cloudflare.com/ajax/libs/kotlin/%s/kotlin.min.js" type="application/javascript"></script>
       |    <script defer="defer" src="%s" type="application/javascript"></script>
       |</head>
       |<body></body>
       |</html>
    """.trimIndent()
