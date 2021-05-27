package ktpack.subprocess

import io.ktor.utils.io.core.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

expect class Process(args: ProcessArguments) {

    val args: ProcessArguments

    val isAlive: Boolean

    val exitCode: Int?

    fun waitFor(): Int

    @ExperimentalTime
    fun waitFor(timeout: Duration): Int?

    val stdin: Output?

    val stdout: Input?

    val stderr: Input?

    fun terminate()

    fun kill()
}

inline fun Process(builder: ProcessArgumentBuilder.() -> Unit) =
    Process(ProcessArguments(builder))
