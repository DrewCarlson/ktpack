package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import ktpack.KtpackOptions

class InitCommand(
    private val term: Terminal
) : CliktCommand(
    help = "Create a new package in an existing directory.",
) {

    override fun run() {
        TODO("Not yet implemented")
    }
}
