package ktpack.commands

import com.github.ajalt.clikt.core.*
import ktpack.*
import ktpack.subprocess.*

class RunCommand : CliktCommand(
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
