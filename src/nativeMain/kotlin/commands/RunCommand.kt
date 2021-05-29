package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.terminal.*
import ktpack.*
import ktpack.subprocess.*
import ktpack.util.*

class RunCommand(private val term: Terminal) : CliktCommand(
    help = "Compile and run binary packages.",
) {

    private val releaseMode by option("--release")
        .help("Run binary in release mode, with optimizations")
        .flag(default = false)

    private val targetBin by option("--bin")
        .help("Run the specified binary")

    override fun run() {
        val manifest = loadManifest(MANIFEST_NAME)
        val moduleBuilder = ModuleBuilder(term, manifest, releaseMode, targetBin)
        val artifactPath = moduleBuilder.build().firstOrNull()
        if (artifactPath.isNullOrBlank()) {
            term.println("${failed("Failed")} no binary to run")
        } else {
            term.println("${success("Running")} `$artifactPath`")
            exec {
                arg(artifactPath)
            }
        }
    }
}
