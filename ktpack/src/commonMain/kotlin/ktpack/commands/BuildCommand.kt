package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.runBlocking
import ktpack.*
import ktpack.configuration.Target
import ktpack.tasks.CompileModuleTask
import ktpack.util.*
import kotlin.system.*

class BuildCommand : CliktCommand(
    help = "Compile packages and dependencies.",
) {
    private val context by requireObject<KtpackContext>()

    private val releaseMode by option("--release")
        .help("Build artifacts in release mode, with optimizations")
        .flag(default = false)

    private val targetBin by option("--bin")
        .help("Build only the specified binary")

    private val libOnly by option("--lib")
        .help("Build only this package's library")
        .flag(default = false)

    private val binsOnly by option("--bins")
        .help("Build all binaries")
        .flag(default = false)

    private val targetPlatform by option("--target")
        .help("The target platform to build for.")
        .enum<Target>()

    override fun run() = runBlocking {
        val manifest = loadManifest(MANIFEST_NAME)
        val module = manifest.module
        val moduleBuilder = ModuleBuilder(module, context.debug)

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

        val target = if (module.targets.size == 1 || module.targets.contains(Target.COMMON_ONLY)) {
            if (module.targets.contains(Target.COMMON_ONLY)) {
                targetPlatform ?: hostTarget
            } else {
                // Make sure we're selecting when the user wants
                val target = module.targets.first()
                if (targetPlatform != null && target != targetPlatform) {
                    context.term.println("${failed("Error")} Selected target '$targetPlatform' but only '$target' is defined")
                    return@runBlocking
                }
                target
            }
        } else if (targetPlatform == null) {
            context.term.println("${failed("Error")} Selected target '$targetPlatform' but can only build for '$hostTarget'")
            return@runBlocking
        } else if (module.targets.contains(targetPlatform)) {
            checkNotNull(targetPlatform)
        } else {
            context.term.println("${failed("Error")} Selected target '$targetPlatform' but choices are '${module.targets.joinToString()}'")
            return@runBlocking
        }

        val results = Dispatchers.Default {
            when {
                libOnly -> listOf(moduleBuilder.buildLib(releaseMode, target))
                binsOnly -> moduleBuilder.buildAllBins(releaseMode, target)
                !targetBin.isNullOrBlank() -> {
                    context.taskRunner.addTask(
                        CompileModuleTask(
                            moduleConf = module,
                            target = checkNotNull(targetBin),
                            releaseMode = releaseMode,
                        )
                    )

                    listOf(moduleBuilder.buildBin(releaseMode, targetBin!!, target))
                }
                else -> moduleBuilder.buildAll(releaseMode, target)
            }
        }

        val failedResults = results.filterIsInstance<ArtifactResult.ProcessError>()
        if (failedResults.isNotEmpty()) {
            context.term.println(
                buildString {
                    append(failed("Failed"))
                    append(" failed to compile selected target(s)")
                    appendLine()
                    failedResults.map { it.message }.forEach(::appendLine)
                }
            )
            exitProcess(-1)
        } else if (results.contains(ArtifactResult.NoArtifactFound)) {
            context.term.println("${failed("Failed")} Could not find artifact to build")
            return@runBlocking
        }

        val totalDuration = results.sumOf { (it as ArtifactResult.Success).compilationDuration }

        context.term.println(
            buildString {
                append(success("Finished"))
                if (releaseMode) {
                    append(" release [optimized] target(s)")
                } else {
                    append(" dev [unoptimized + debuginfo] target(s)")
                }
                append(" in ${totalDuration}s")
            }
        )
    }
}
