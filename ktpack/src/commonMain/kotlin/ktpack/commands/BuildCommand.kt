package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.rendering.TextColors.cyan
import kotlinx.coroutines.runBlocking
import ktpack.*
import ktpack.configuration.ModuleConf
import ktpack.configuration.Target
import ktpack.util.*
import kotlin.system.*

class BuildCommand : CliktCommand(
    help = "Compile packages and dependencies.",
) {
    private val context by requireObject<CliContext>()

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

    private val userTarget by option("--target", "-t")
        .help("The target platform to build for.")
        .enum<Target>()

    private val allTargets by option("--all-targets", "-a")
        .help("Build all targets supported by this host.")
        .flag()

    override fun run() = runBlocking {
        val manifest = loadManifest(MANIFEST_NAME)
        val module = manifest.module
        val moduleBuilder = ModuleBuilder(module, context)

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

        val targetBuildList = if (allTargets) {
            if (module.targets.contains(Target.COMMON_ONLY)) {
                getHostSupportedTargets()
            } else {
                getHostSupportedTargets().filter(module.targets::contains)
            }
        } else {
            listOf(module.validateTargetOrAlternative(context, hostTarget, userTarget) ?: return@runBlocking)
        }
        context.term.println("${info("Building")} for ${targetBuildList.joinToString { verbose(it.name.lowercase()) }}")
        val results: List<ArtifactResult> =
            targetBuildList
                .flatMap { target ->
                    context.term.println("${info("Building")} Starting compilation for ${verbose(target.name.lowercase())}")
                    when {
                        !targetBin.isNullOrBlank() -> listOf(moduleBuilder.buildBin(releaseMode, targetBin!!, target))
                        libOnly -> listOf(moduleBuilder.buildLib(releaseMode, target))
                        binsOnly -> moduleBuilder.buildAllBins(releaseMode, target)
                        else -> moduleBuilder.buildAll(releaseMode, target)
                    }.onEach { artifact ->
                        when (artifact) {
                            is ArtifactResult.Success -> logArtifactSuccess(artifact)
                            is ArtifactResult.ProcessError -> {
                                logArtifactError(artifact)
                                exitProcess(-1)
                            }
                            ArtifactResult.NoArtifactFound -> Unit // Ignore, handle when all artifacts a resolved
                        }
                    }
                }
                .toList()

        if (results.all { it == ArtifactResult.NoArtifactFound }) {
            context.term.println("${failed("Failed")} Could not find artifact to build")
            return@runBlocking
        }

        val totalDuration = results
            .filterIsInstance<ArtifactResult.Success>()
            .sumOf { it.compilationDuration }

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

    private fun logArtifactError(artifact: ArtifactResult.ProcessError) {
        context.term.println(
            buildString {
                append(failed("Failed"))
                append(" failed to compile selected target(s)")
                appendLine()
                artifact.message?.lines()?.forEach(::appendLine)
            }
        )
    }

    private fun logArtifactSuccess(artifact: ArtifactResult.Success) {
        context.term.println(
            buildString {
                append(cyan("Building"))
                append(" Completed build for ")
                append(verbose(artifact.target.name.lowercase()))
                append(" in ${artifact.compilationDuration}s")
            }
        )
    }

    /**
     * Produce a list of [Target]s that are supported as build
     * targets for the host system.
     */
    private fun getHostSupportedTargets() = Target.values().filter { target ->
        when (target) {
            Target.COMMON_ONLY -> false
            Target.JVM,
            Target.JS_NODE,
            Target.JS_BROWSER -> true

            Target.MACOS_ARM64,
            Target.MACOS_X64 -> Platform.osFamily == OsFamily.MACOSX

            Target.WINDOWS_X64 -> Platform.osFamily == OsFamily.WINDOWS ||
                    Platform.osFamily == OsFamily.MACOSX

            Target.LINUX_X64 -> Platform.osFamily == OsFamily.LINUX ||
                    Platform.osFamily == OsFamily.MACOSX ||
                    Platform.osFamily == OsFamily.WINDOWS
        }
    }
}

/**
 * Given a non-null [requestedTarget], ensure that [hostTarget]
 * supports building for the target or return null.  When no
 * target is requested ([requestedTarget] is null), return a
 * target supported by [hostTarget] or null.
 */
fun ModuleConf.validateTargetOrAlternative(
    context: CliContext,
    hostTarget: Target,
    requestedTarget: Target?,
): Target? {
    return if (targets.size == 1 || targets.contains(Target.COMMON_ONLY)) {
        if (targets.contains(Target.COMMON_ONLY)) {
            requestedTarget ?: hostTarget
        } else {
            // Make sure we're selecting when the user wants
            val target = targets.first()
            if (requestedTarget != null && target != requestedTarget) {
                context.term.println("${failed("Error")} Selected target '$requestedTarget' but only '$target' is defined")
                null
            } else {
                target
            }
        }
    } else if (requestedTarget == null) {
        context.term.println("${failed("Error")} Selected target '$requestedTarget' but can only build for '$hostTarget'")
        null
    } else if (targets.contains(requestedTarget)) {
        requestedTarget
    } else {
        context.term.println("${failed("Error")} Selected target '$requestedTarget' but choices are '${targets.joinToString()}'")
        null
    }
}
