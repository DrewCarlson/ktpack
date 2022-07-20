package ktpack.logging

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import ktpack.util.verbose

fun Terminal.Logger(
    tag: String,
    tagColors: TextColors = TextColors.white,
): Logger {
    return DefaultLogger(LogContext(tag, tagColors), this)
}

class DefaultLogger(
    private val context: LogContext,
    private val term: Terminal,
) : Logger {

    private val infoTag = verbose("I/")
    private val debugTag = verbose("D/")
    private val traceTag = verbose("T/")
    private val warnTag = verbose("W/")
    private val errorTag = verbose("E/")

    override fun info(message: String, vararg objects: Any) {
        log(infoTag, message, objects)
    }

    override fun debug(message: String, vararg objects: Any) {
        log(debugTag, message, objects)
    }

    override fun trace(message: String, vararg objects: Any) {
        log(traceTag, message, objects)
    }

    override fun warn(message: String, vararg objects: Any) {
        log(warnTag, message, objects)
    }

    override fun error(message: String, vararg objects: Any) {
        log(errorTag, message, objects)
    }

    private fun log(tag: String, message: String, objects: Array<out Any>) {
        term.println("$tag${context.tagColor(context.tag)} $message")
        val firstObject = objects.firstOrNull()
        if (firstObject is Throwable) {
            log(tag, firstObject.stackTraceToString(), emptyArray())
        }
        // TODO: log other objects
    }
}
