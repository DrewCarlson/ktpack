package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*

class KtpackCommand(private val term: Terminal) : CliktCommand(
    help = "Build, package, and distribute Kotlin software with ease."
) {

    override fun run() = Unit
}
