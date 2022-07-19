package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import ktpack.util.failed
import ktpack.util.warn
import kotlin.system.exitProcess

class VersionCommand(
    private val term: Terminal
) : CliktCommand(
    help = "Show Ktpack version information."
) {
    override fun run() {
        term.println(
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
