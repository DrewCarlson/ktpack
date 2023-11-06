package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import ktpack.*
import ktpack.compilation.ArtifactResult
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
import ktpack.util.*

class BuildCommand : CliktCommand(
    help = "Compile packages and dependencies.",
) {
    private val logger = Logger.withTag(BuildCommand::class.simpleName.orEmpty())
    private val context by requireObject<CliContext>()

    private val releaseMode by option("--release", "-r")
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

        logger.i {
            "{} {} v{} ({})".format(
                success("Compiling"),
                packageConf.module.name,
                packageConf.module.version,
                moduleBuilder.modulePath,
            )
        }

        val targetBuildList = if (allTargets) {
            if (module.targets.isEmpty()) {
                PlatformUtils.getHostSupportedTargets()
            } else {
                PlatformUtils.getHostSupportedTargets().filter(module.targets::contains)
            }
        } else {
            listOf(module.validateTargetOrAlternative(context, userTarget) ?: return@runBlocking)
        }
        logger.i {
            "{} Selected target(s): {}".format(
                info("Building"),
                targetBuildList.joinToString { verbose(it.name.lowercase()) },
            )
        }
        val results: List<ArtifactResult> =
            targetBuildList
                .flatMap { target ->
                    terminal.loadingIndeterminate {
                        buildAllForTarget(moduleBuilder, target)
                    }
                }
                .toList()

        if (results.all { it == ArtifactResult.NoArtifactFound }) {
            logger.i { "${failed("Failed")} Could not find artifact to build" }
            return@runBlocking
        }

        val totalDuration = results
            .filterIsInstance<ArtifactResult.Success>()
            .sumOf { it.compilationDuration }

        logger.i {
            "{} {} target(s) in {}s".format(
                success("Finished"),
                if (releaseMode) "release [optimized]" else "dev [unoptimized + debuginfo]",
                totalDuration
            )
        }
    }

    private suspend fun buildAllForTarget(
        moduleBuilder: ModuleBuilder,
        target: KotlinTarget,
    ): List<ArtifactResult> {
        val targetFormat = "{} Selected build types: {} {}"
        return when {
            !targetBin.isNullOrBlank() -> {
                logger.i { targetFormat.format(info("Building"), "bin", verbose(targetBin!!)) }
                listOf(moduleBuilder.buildBin(releaseMode, targetBin!!, target))
            }

            libOnly -> {
                logger.i { targetFormat.format(info("Building"), "lib", "") }
                context.term.println("lib ${verbose(target.name.lowercase())}")
                listOf(moduleBuilder.buildLib(releaseMode, target))
            }

            binsOnly -> {
                logger.i { targetFormat.format(info("Building"), "all bins", "") }
                moduleBuilder.buildAllBins(releaseMode, target)
            }

            else -> {
                logger.i { targetFormat.format(info("Building"), "all bins and libs", "") }
                moduleBuilder.buildAll(releaseMode, target)
            }
        }.onEach { artifact ->
            processArtifact(artifact)
        }
    }

    private fun processArtifact(artifact: ArtifactResult) {
        when (artifact) {
            is ArtifactResult.Success -> {
                logger.i {
                    "{} Completed build for {} in {}s".format(
                        info("Building"),
                        verbose(artifact.target.name.lowercase()),
                        artifact.compilationDuration,
                    )
                }
            }

            is ArtifactResult.ProcessError -> {
                logger.i { "${failed("Failed")} failed to compile selected target(s)" }
                if (!artifact.message.isNullOrBlank()) {
                    logger.i(artifact.message)
                }
                exitProcess(1)
            }

            ArtifactResult.NoArtifactFound -> Unit // Ignore, handle when all artifacts a resolved
            ArtifactResult.NoSourceFiles -> Unit
        }
    }
}
