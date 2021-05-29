package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import ktpack.*
import ktpack.subprocess.*

class RunCommand(private val term: Terminal) : CliktCommand(
    help = "Compile and run binary packages.",
) {
    override fun run() {
        val manifest = loadManifest(MANIFEST_NAME)
        val result = exec {
            // TODO: actual binary artifact lookup
            arg("./out/main.kexe")
        }
    }
}
