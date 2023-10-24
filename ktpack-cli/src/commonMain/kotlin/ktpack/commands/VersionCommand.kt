package ktpack.commands

import com.github.ajalt.clikt.core.*
import ktpack.CliContext
import ktpack.Ktpack
import ktpack.util.info
import ktpack.util.verbose

class VersionCommand : CliktCommand(
    help = "Show Ktpack version information.",
) {
    private val context by requireObject<CliContext>()

    override fun run() {
        context.term.println("${verbose("Ktpack")} version ${info(Ktpack.VERSION)}")
        context.term.println("${verbose("Kotlin")} version ${info(Ktpack.KOTLIN_VERSION)}")
        context.term.println("${verbose("Coroutines")} version ${info(Ktpack.COROUTINES_VERSION)}")
        context.term.println("${verbose("Ktor")} version ${info(Ktpack.KTOR_VERSION)}")
        context.term.println("${verbose("Serialization")} version ${info(Ktpack.SERIALIZATION_VERSION)}")
        context.term.println("${verbose("Build")} sha ${info(Ktpack.BUILD_SHA)}")
        context.term.println("${verbose("Build")} date ${info(Ktpack.BUILD_DATE)}")
    }
}
