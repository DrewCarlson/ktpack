package ktpack.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.terminal.Terminal

class DependenciesCommand(
    private val term: Terminal
) : CliktCommand(
    help = "Manage project dependencies.",
) {
    override fun run() {
        TODO("Not yet implemented")
    }
}
