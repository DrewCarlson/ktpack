package ktpack.util

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal

private const val LOG = "[{}] {}: {}"
private val VERBOSE = verbose("Verbose")
private val DEBUG = verbose("Debug")
private val WARN = warn("Warm")
private val ERROR = failed("Error")
private val ASSERT = failed("Assert")

class MordantLogWriter(
    private val term: Terminal,
) : LogWriter() {

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val line = when (severity) {
            Severity.Info -> message
            Severity.Verbose -> LOG.format(VERBOSE, bold(tag), message)
            Severity.Debug -> LOG.format(DEBUG, bold(tag), message)
            Severity.Warn -> LOG.format(WARN, bold(tag), message)
            Severity.Error -> LOG.format(ERROR, bold(tag), message)
            Severity.Assert -> LOG.format(ASSERT, bold(tag), message)
        }
        term.println(line)
        if (throwable != null) {
            term.println(throwable.stackTraceToString())
        }
    }
}

fun String.format(vararg args: Any?): String {
    if (!contains("{}")) {
        return this
    }
    val formatParts = split("{}", limit = args.size + 1)
    return buildString(length) {
        for (i in args.indices) {
            append(formatParts[i])
            append(args[i])
        }

        append(formatParts.last())

    }
}
