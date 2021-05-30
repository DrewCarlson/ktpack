package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.terminal.*
import ktpack.*
import ktpack.util.*
import kotlin.system.*
import kotlin.time.*

class BuildCommand(private val term: Terminal) : CliktCommand(
    help = "Compile packages and dependencies.",
) {

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

    override fun run() {
        val manifest = loadManifest(MANIFEST_NAME)
        val module = manifest.module
        val moduleBuilder = ModuleBuilder(module)

        term.println(buildString {
            append(success("Compiling"))
            append(" ${manifest.module.name}")
            append(" v${manifest.module.version}")
            append(" (${moduleBuilder.srcFolder.getParent()})")
        })

        val results = when {
            libOnly -> moduleBuilder.buildLib(releaseMode)
            binsOnly -> moduleBuilder.buildAllBins(releaseMode)
            !targetBin.isNullOrBlank() -> listOf(moduleBuilder.buildBin(releaseMode, targetBin!!))
            else -> moduleBuilder.buildAll(releaseMode)
        }

        val failedResults = results.filterIsInstance<ArtifactResult.Error>()
        if (failedResults.isNotEmpty()) {
            term.println(buildString {
                append(failed("Failed"))
                append(" failed to compile selected target(s)")
                appendLine()
                failedResults.forEach(::appendLine)
            })
            exitProcess(-1)
        }

        val totalDuration = results.sumOf { (it as ArtifactResult.Success).compilationDuration }

        term.println(buildString {
            append(success("Finished"))
            if (releaseMode) {
                append(" release [optimized] target(s)")
            } else {
                append(" dev [unoptimized + debuginfo] target(s)")
            }
            append(" in ${totalDuration}s")
        })
    }
}
