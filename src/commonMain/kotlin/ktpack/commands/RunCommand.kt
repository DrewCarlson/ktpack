package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.terminal.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ksubprocess.*
import ktpack.*
import ktpack.util.*

class RunCommand(
    private val term: Terminal
) : CliktCommand(
    help = "Compile and run binary packages.",
) {

    private val ktpackOptions by requireObject<KtpackOptions>()

    private val releaseMode by option("--release")
        .help("Run binary in release mode, with optimizations")
        .flag(default = false)

    private val targetBin by option("--bin")
        .help("Run the specified binary")

    override fun run(): Unit = runBlocking {
        val manifest = loadManifest(MANIFEST_NAME)
        val moduleBuilder = ModuleBuilder(manifest.module, ktpackOptions.debug)
        val targetBin = targetBin ?: manifest.module.name

        term.println(
            buildString {
                append(success("Compiling"))
                append(" ${manifest.module.name}")
                append(" v${manifest.module.version}")
                append(" (${moduleBuilder.srcFolder.getParent()})")
            }
        )
        when (val result = moduleBuilder.buildBin(releaseMode, targetBin)) {
            is ArtifactResult.Success -> {
                if (ktpackOptions.debug) {
                    term.println(result.outputText)
                }
                term.println(
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
                term.println("${success("Running")} '${result.artifactPath}'")
                try {
                    val (exitCode, duration) = measureSeconds {
                        val infoLogKey = info("$targetBin|out")
                        val errorLogKey = failed("$targetBin|err")
                        Process {
                            arg(result.artifactPath)
                        }.run {
                            Dispatchers.Default {
                                val writeLock = Mutex()
                                merge(
                                    stdoutLines.map { "$infoLogKey $it" },
                                    stderrLines.map { "$errorLogKey $it" },
                                ).collect { line ->
                                    writeLock.withLock { term.println(line) }
                                }
                                waitFor()
                            }
                        }
                    }
                    if (exitCode == 0) {
                        term.println("${success("Finished")} Program completed successfully in ${duration}s")
                    } else {
                        term.println("${failed("Failed")} Program terminated with code ($exitCode) in ${duration}s")
                    }
                } catch (e: IOException) {
                    term.println("${failed("Failed")} Program could not be started due to an IO error")
                    if (ktpackOptions.stacktrace) {
                        term.println((e.cause ?: e).stackTraceToString())
                    }
                }
            }
            is ArtifactResult.ProcessError -> {
                term.println("${failed("Failed")} Compilation process failed with exit code (${result.exitCode})")
                term.println(result.message.orEmpty())
            }
            is ArtifactResult.NoArtifactFound -> {
                term.println("${failed("Failed")} no binary to run")
            }
        }
    }
}
