package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.*
import ktpack.KtpackOptions
import ktpack.util.info
import ktpack.util.verbose

class KtpackCommand(
    private val term: Terminal,
) : CliktCommand(
    help = "Build, package, and distribute Kotlin software with ease."
) {

    val stacktrace: Boolean by option(help = "Print the stacktrace in the case of an unhandled exception.")
        .flag()
    val debug: Boolean by option().flag()

    override fun aliases(): Map<String, List<String>> = mapOf(
        "ktver" to listOf("kotlin-versions"),
    )

    override fun run() {
        currentContext.obj = KtpackOptions(stacktrace, debug)
        term.println(
            buildString {
                append(info("Ktpack"))
                append(" ")
                append(verbose("v1.0.0-SNAPSHOT"))
                if (debug) {
                    append(" [Debug Mode]")
                }
            }
        )
    }
}
