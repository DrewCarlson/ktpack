package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import ktpack.Ktpack
import ktpack.CliContext
import ktpack.task.TaskRunner
import ktpack.util.info
import ktpack.util.verbose

class KtpackCommand(
    override val term: Terminal,
) : CliktCommand(
    help = "A simple tool for building and publishing Kotlin software."
), CliContext {

    override val http: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(ktpack.json)
            }
        }
    }

    override val stacktrace: Boolean by option()
        .help("Print the stacktrace in the case of an unhandled exception.")
        .flag()
    override val debug: Boolean by option().flag()

    override fun aliases(): Map<String, List<String>> = emptyMap()

    override val taskRunner: TaskRunner = TaskRunner()

    override fun run() {
        currentContext.obj = this
        if (debug) {
            term.println("${info("Ktpack")} ${verbose(Ktpack.VERSION)}")
        }
    }
}
