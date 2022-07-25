package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import ktpack.task.TaskRunner

interface CliContext {
    val stacktrace: Boolean
    val debug: Boolean
    val taskRunner: TaskRunner
    val http: HttpClient
    val term: Terminal
}