package ktpack.subprocess

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlin.time.*

data class CommunicateResult(
    val exitCode: Int,
    val output: String,
    val errors: String
) {

    fun check() {
        if (exitCode != 0) throw ProcessExitException(this)
    }
}

@OptIn(ExperimentalTime::class)
fun Process.communicate(
    input: String = "",
    charset: Charset = Charsets.UTF_8,
    timeout: Duration? = null,
    killTimeout: Duration? = null
): CommunicateResult {
    val stdoutCollector =
        if (args.stdout == Redirect.Pipe)
            BackgroundPipeCollector(this, false, charset)
        else null
    val stderrCollector =
        if (args.stderr == Redirect.Pipe)
            BackgroundPipeCollector(this, true, charset)
        else null

    stdin?.let {
        it.writeText(input, charset = charset)
        it.close()
    }

    if (timeout != null && waitFor(timeout) == null) {
        if (killTimeout == Duration.ZERO) {
            kill()
        } else {
            terminate()

            if (killTimeout != null && waitFor(killTimeout) == null) {
                kill()
            }
        }
    }

    val exitCode = waitFor()

    BackgroundPipeCollector.awaitAll(listOfNotNull(stdoutCollector, stderrCollector))

    return CommunicateResult(
        exitCode,
        stdoutCollector?.result ?: "",
        stderrCollector?.result ?: ""
    )
}

internal expect class BackgroundPipeCollector(
    process: Process,
    isStderr: Boolean,
    charset: Charset
) {
    fun await()

    val result: String

    companion object {
        fun awaitAll(readers: List<BackgroundPipeCollector>)
    }
}

