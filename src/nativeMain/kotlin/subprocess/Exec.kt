package ktpack.subprocess

import io.ktor.utils.io.charsets.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class ExecArgumentsBuilder : ProcessArgumentBuilder() {
    var input: String = ""

    var charset: Charset = Charsets.UTF_8

    @ExperimentalTime
    var timeout: Duration? = null
        set(value) {
            require(value == null || value.isPositive()) {
                "timeout must be positive."
            }
            field = if (value?.isInfinite() == true) null else value
        }

    @ExperimentalTime
    var killTimeout: Duration? = null
        set(value) {
            require(value == null || (value.isPositive() || value == Duration.ZERO)) {
                "killTimeout timeout must be >= 0"
            }
            field = if (value?.isInfinite() == true) null else value
        }

    var check = false

    inline fun input(builder: StringBuilder.() -> Unit) {
        input = buildString(builder)
    }
}

@OptIn(ExperimentalTime::class)
inline fun exec(builder: ExecArgumentsBuilder.() -> Unit): CommunicateResult {
    val rab = ExecArgumentsBuilder()
    rab.builder()

    val proc = Process(rab.build())

    val res = proc.communicate(
        rab.input,
        rab.charset,
        rab.timeout,
        rab.killTimeout
    )

    if (rab.check) res.check()

    return res
}
