package ktpack.commands

import com.github.ajalt.clikt.core.*

class KtpackCommand : CliktCommand(
    help = "Build, package, and distribute Kotlin software with ease."
) {

    override fun run() = Unit
}
