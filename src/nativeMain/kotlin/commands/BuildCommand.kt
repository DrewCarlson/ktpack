package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.terminal.*
import ktpack.*
import ktpack.util.*

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
        val moduleBuilder = ModuleBuilder(term, manifest, releaseMode, targetBin)
        moduleBuilder.build()
    }
}
