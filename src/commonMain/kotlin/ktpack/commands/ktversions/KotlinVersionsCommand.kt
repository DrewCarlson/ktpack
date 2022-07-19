package ktpack.commands.ktversions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*

class KotlinVersionsCommand(
    private val term: Terminal
) : CliktCommand(
    name = "kotlin-versions",
    help = "Install and manage Kotlin Compiler versions.",
) {

    override fun run() {
    }
}
