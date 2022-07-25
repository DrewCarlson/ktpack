package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ksubprocess.*
import ktfio.File
import ktpack.*
import ktpack.commands.jdk.JdkDistribution
import ktpack.commands.jdk.JdkInstalls
import ktpack.configuration.Target
import ktpack.util.*

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
        .enum<Target>()

    override fun run(): Unit = runBlocking {
        val manifest = loadManifest(MANIFEST_NAME)
        val moduleBuilder = ModuleBuilder(manifest.module, context)
        val targetBin = targetBin ?: manifest.module.name

        context.term.println(
            buildString {
                append(success("Compiling"))
                append(" ${manifest.module.name}")
                append(" v${manifest.module.version}")
                append(" (${moduleBuilder.srcFolder.getParent()})")
            }
        )
        val hostTarget = when (Platform.osFamily) {
            OsFamily.MACOSX -> if (Platform.cpuArchitecture == CpuArchitecture.ARM64) {
                Target.MACOS_ARM64
            } else {
                Target.MACOS_X64
            }
            OsFamily.LINUX -> Target.LINUX_X64
            OsFamily.WINDOWS -> Target.WINDOWS_X64
            else -> error("Unsupported host operating system")
        }

        val target = manifest.module.validateTargetOrAlternative(context, hostTarget, userTarget) ?: return@runBlocking
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
                        val infoLogKey = info("$targetBin|out")
                        val errorLogKey = failed("$targetBin|err")
                        Process {
                            when (target) {
                                Target.COMMON_ONLY -> error("")
                                Target.JVM -> {
                                    val jdkInstallation = JdkInstalls.findJdk(JdkInstalls.defaultJdksRoot, "11", JdkDistribution.Zulu)
                                    if (jdkInstallation == null) {
                                        context.term.println("${failed("Failed")} Could not find JDK installation.")
                                        return@runBlocking
                                    }
                                    arg(File(jdkInstallation.path, "bin", "java").getAbsolutePath())
                                    arg("-cp")
                                    arg(result.artifactPath)
                                    arg("MainKt") // TODO: get main class from artifact
                                }
                                Target.JS_NODE -> {
                                    arg("C:\\Users\\drewc\\.gradle\\nodejs\\node-v16.13.0-win-x64\\node.exe")
                                    arg(result.artifactPath)
                                }
                                Target.JS_BROWSER -> TODO()
                                else -> {
                                    // native targets
                                    arg(result.artifactPath)
                                }
                            }
                        }.run {
                            Dispatchers.Default {
                                val writeLock = Mutex()
                                merge(
                                    stdoutLines.map { "$infoLogKey $it" },
                                    stderrLines.map { "$errorLogKey $it" },
                                ).collect { line ->
                                    writeLock.withLock { context.term.println(line) }
                                }
                                waitFor()
                            }
                        }
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
}
