package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.terminal.*
import com.github.xfel.ksubprocess.*
import ktpack.*
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
        val moduleBuilder = ModuleBuilder(manifest.module)
        val targetBin = targetBin ?: manifest.module.name

        term.println(buildString {
            append(success("Compiling"))
            append(" ${manifest.module.name}")
            append(" v${manifest.module.version}")
            append(" (${moduleBuilder.srcFolder.getParent()})")
        })
        when (val result = moduleBuilder.buildBin(releaseMode, targetBin)) {
            is ArtifactResult.Success -> {
                term.println(buildString {
                    append(success("Finished"))
                    if (releaseMode) {
                        append(" release [optimized] target(s)")
                    } else {
                        append(" dev [unoptimized + debuginfo] target(s)")
                    }
                    append(" in ${result.compilationDuration}s")
                })
                term.println("${success("Running")} `${result.artifactPath}`")
                exec {
                    arg(result.artifactPath)
                }
            }
            is ArtifactResult.Error -> TODO()
            null -> {
                term.println("${failed("Failed")} no binary to run")
            }
        }
    }
}
