package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import ktpack.*
import ktpack.compilation.ArtifactResult
import ktpack.compilation.KtpackSourceCollector
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
import ktpack.util.*

class BuildCommand : CliktCommand() {
    override fun help(context: Context): String {
        return context. theme. info("Compile packages and dependencies.")
    }

    private val logger = Logger.withTag(BuildCommand::class.simpleName.orEmpty())
    private val context by requireObject<CliContext>()

    private val releaseMode by option("--release", "-r")
        .help("Build artifacts in release mode, with optimizations")
        .flag(default = false)

    private val targetBin by option("--bin")
        .help("Build only the specified binary")

    private val userTarget by option("--target", "-t")
        .help("The target platform to build for.")
        .enum<KotlinTarget>()

    private val allTargets by option("--all-targets", "-a")
        .help("Build all targets supported by this host.")
        .flag()

    override fun run() = runBlocking {
        val manifest = context.loadManifestToml()
        val module = manifest.module
        val output =
            manifest.module.output ?: KtpackSourceCollector(Path(workingDirectory, "src")).getDefaultOutput(userTarget)
        val moduleBuilder = ModuleBuilder(manifest, context, workingDirectory)

        logger.i {
            "{} {} v{} ({})".format(
                success("Compiling"),
                module.name,
                module.version,
                moduleBuilder.modulePath,
            )
        }

        val target = if (allTargets) {
            val hostTargets = PlatformUtils.getHostSupportedTargets()
            output.targets.first { hostTargets.contains(it) }
        } else {
            val alternateTarget = output.validateTargetOrAlternative(context, userTarget)
            checkNotNull(alternateTarget) { "Failed to select alternateTarget" }
        }
        logger.i {
            "{} Selected target(s): {}".format(
                info("Building"),
                verbose(target.name.lowercase()),
            )
        }
        val result = terminal.loadingIndeterminate {
            buildAllForTarget(moduleBuilder, target)
        }

        if (result is ArtifactResult.NoArtifactFound) {
            logger.i { "${failed("Failed")} Could not find artifact to build" }
            return@runBlocking
        }

        val totalDuration = (result as ArtifactResult.Success).compilationDuration

        logger.i {
            "{} {} target(s) in {}s".format(
                success("Finished"),
                if (releaseMode) "release [optimized]" else "dev [unoptimized + debuginfo]",
                totalDuration,
            )
        }
    }

    private suspend fun buildAllForTarget(
        moduleBuilder: ModuleBuilder,
        target: KotlinTarget,
    ): ArtifactResult {
        val targetFormat = "{} Selected build types: {} {}"
        logger.i { targetFormat.format(info("Building"), "all bins and libs", "") }

        return moduleBuilder.build(releaseMode, target).also(::processArtifact)
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
