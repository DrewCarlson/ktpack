package ktpack.commands

import com.github.ajalt.clikt.core.*
import ktpack.KtpackContext
import ktpack.util.failed
import ktpack.util.warn
import kotlin.system.exitProcess

class VersionCommand : CliktCommand(
    help = "Show Ktpack version information."
) {
    private val context by requireObject<KtpackContext>()

    override fun run() {
        context.term.println(
            buildString {
                append(failed("Failed"))
                append(" Command ")
                append(warn(this@VersionCommand.commandName))
                append(" is not implemented.")
            }
        )
        exitProcess(-1)
    }
}
