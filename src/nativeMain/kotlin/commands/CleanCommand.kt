package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*

class CleanCommand(private val term: Terminal) : CliktCommand(
    help = "Remove generated artifacts and folders.",
) {
    override fun run() {
        TODO("Not yet implemented")
    }
}
