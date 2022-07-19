package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*

class TestCommand(
    private val term: Terminal
) : CliktCommand(
    help = "Compile and run test suites.",
) {
    override fun run() {
        TODO("Not yet implemented")
    }
}
