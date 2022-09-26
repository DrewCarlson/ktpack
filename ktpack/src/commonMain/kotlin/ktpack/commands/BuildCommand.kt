package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.rendering.TextColors.cyan
import kotlinx.coroutines.runBlocking
import ktpack.*
import ktpack.compilation.ArtifactResult
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
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
        .enum<KotlinTarget>()

    private val allTargets by option("--all-targets", "-a")
        .help("Build all targets supported by this host.")
        .flag()

    override fun run() = runBlocking {
        val packageConf = context.loadKtpackConf()
        val module = packageConf.module
        val moduleBuilder = ModuleBuilder(module, context, workingDirectory)

        context.term.println(
            buildString {
                append(success("Compiling"))
                append(" ${packageConf.module.name}")
                append(" v${packageConf.module.version}")
                append(" (${moduleBuilder.srcFolder.getParent()})")
            }
        )

        val targetBuildList = if (allTargets) {
            if (module.targets.isEmpty()) {
                PlatformUtils.getHostSupportedTargets()
            } else {
                PlatformUtils.getHostSupportedTargets().filter(module.targets::contains)
            }
        } else {
            listOf(module.validateTargetOrAlternative(context, userTarget) ?: return@runBlocking)
        }
        context.term.println("${info("Building")} for ${targetBuildList.joinToString { verbose(it.name.lowercase()) }}")
        val results: List<ArtifactResult> =
            targetBuildList
                .flatMap { target ->
                    context.term.print("${info("Building")} Starting compilation for ")
                    when {
                        !targetBin.isNullOrBlank() -> {
                            context.term.println("bin ${verbose(targetBin!!)} ${verbose(target.name.lowercase())}")
                            listOf(moduleBuilder.buildBin(releaseMode, targetBin!!, target))
                        }
                        libOnly -> {
                            context.term.println("lib ${verbose(target.name.lowercase())}")
                            listOf(moduleBuilder.buildLib(releaseMode, target))
                        }
                        binsOnly -> {
                            context.term.println("all bins ${verbose(target.name.lowercase())}")
                            moduleBuilder.buildAllBins(releaseMode, target)
                        }
                        else -> {
                            context.term.println("all bins and libs ${verbose(target.name.lowercase())}")
                            moduleBuilder.buildAll(releaseMode, target)
                        }
                    }.onEach { artifact ->
                        when (artifact) {
                            is ArtifactResult.Success -> logArtifactSuccess(artifact)
                            is ArtifactResult.ProcessError -> {
                                logArtifactError(artifact)
                                exitProcess(1)
                            }

                            ArtifactResult.NoArtifactFound -> Unit // Ignore, handle when all artifacts a resolved
                            ArtifactResult.NoSourceFiles -> Unit
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
}
