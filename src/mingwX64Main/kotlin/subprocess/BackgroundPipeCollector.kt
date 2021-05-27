package ktpack.subprocess

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import ktpack.subprocess.win.Input
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.waitForMultipleFutures

/**
 * Reads the given stream's text in the background, used by communicate.
 *
 * Backed by kotlin/native threading.
 */
internal actual class BackgroundPipeCollector actual constructor(
    process: Process,
    isStderr: Boolean,
    charset: Charset
) {
    private val worker = Worker.start()
    @OptIn(ExperimentalIoApi::class)
    private val future = worker.execute(TransferMode.SAFE, {
        val stream =
            if (isStderr) Input(process.stderrFd)
            else Input(process.stdoutFd)
        Pair(stream, charset.name)
    }) { (stream, csn) ->
        try {
            stream.readText(charset = Charset.forName(csn))
        } finally {
            stream.close()
        }
    }


    actual fun await() {
        waitForMultipleFutures(listOf(future), WAIT_TIMEOUT)
        worker.requestTermination(false)
    }

    actual val result: String
        get() = future.result

    actual companion object {
        // this value is arbitrary, I'd take infinite if possible
        private const val WAIT_TIMEOUT = 10000

        actual fun awaitAll(readers: List<BackgroundPipeCollector>) {
            waitForMultipleFutures(readers.map { it.future }, WAIT_TIMEOUT)
            readers.forEach {
                it.worker.requestTermination(false)
            }
        }
    }
}
