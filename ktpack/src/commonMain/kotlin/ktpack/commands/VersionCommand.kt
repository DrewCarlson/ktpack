package ktpack.commands

import com.github.ajalt.clikt.core.*
import ktpack.CliContext
import ktpack.Ktpack
import ktpack.util.info
import ktpack.util.verbose

class VersionCommand : CliktCommand(
    help = "Show Ktpack version information."
) {
    private val context by requireObject<CliContext>()

    override fun run() {
        context.term.println("${verbose("Ktpack")} version ${info(Ktpack.VERSION)}")
    }
}
