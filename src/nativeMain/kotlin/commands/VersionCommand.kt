package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*

class VersionCommand(private val term: Terminal) : CliktCommand(
    help = "Show Ktpack version information."
) {
    override fun run() {
        TODO("Not yet implemented")
    }
}
