package ktpack.util

import ktpack.CliContext

fun CliContext.logError(exception: Exception) {
    if (stacktrace) {
        term.println((exception.cause ?: exception).stackTraceToString())
    }
}
