package ktpack.subprocess

open class ProcessException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class ProcessExitException(val result: CommunicateResult) : ProcessException(buildString {
    append("Process exited with exit code ")
    append(result.exitCode)

    if (result.errors.isNotBlank()) {
        append(": ")
        val errLines = result.errors.lines()
        val errLinesNotBlank = errLines.filter { it.isNotBlank() }
        if (errLinesNotBlank.size == 1) {
            append(errLinesNotBlank.single())
        } else {
            for (line in errLines) {
                append("\n    ")
                append(line)
            }
        }
    }
})

class ProcessConfigException(message: String? = null, cause: Throwable? = null) : ProcessException(message, cause)
