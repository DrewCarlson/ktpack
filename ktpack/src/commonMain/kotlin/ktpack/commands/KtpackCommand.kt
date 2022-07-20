package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import ktpack.Ktpack
import ktpack.KtpackContext
import ktpack.task.TaskRunner
import ktpack.util.info
import ktpack.util.verbose

class KtpackCommand(
    override val term: Terminal,
    override val http: HttpClient,
) : CliktCommand(
    help = "Build, package, and distribute Kotlin software with ease."
),
    KtpackContext {

    override val stacktrace: Boolean by option(
        help = "Print the stacktrace in the case of an unhandled exception."
    ).flag()
    override val debug: Boolean by option().flag()

    override fun aliases(): Map<String, List<String>> = mapOf(
        "kotlin" to listOf("kotlin-versions"),
        "jdk" to listOf("jdk-versions"),
    )

    override val taskRunner: TaskRunner = TaskRunner()

    override fun run() {
        currentContext.obj = this
        if (debug) {
            term.println("${info("Ktpack")} ${verbose(Ktpack.VERSION)}")
        }
    }
}
