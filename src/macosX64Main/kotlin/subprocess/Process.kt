package ktpack.subprocess

import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
import io.ktor.utils.io.streams.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.Foundation.*
import platform.posix.*
import kotlin.native.concurrent.*
import kotlin.time.*

private fun Int.closeFd() {
    if (this != -1) close(this)
}

private data class RedirectFds(val readFd: Int, val writeFd: Int) {
    constructor(fd: Int, isRead: Boolean) : this(
        if (isRead) fd else -1,
        if (isRead) -1 else fd
    )
}

@OptIn(ExperimentalIoApi::class)
private fun Redirect.openFds(stream: String): RedirectFds = when (this) {
    Redirect.Null -> {
        val fd = open("/dev/null", O_RDWR)
        if (fd == -1) {
            throw ProcessConfigException(
                "Error opening null file for $stream",
                PosixException.forErrno(posixFunctionName = "open()")
            )
        }
        RedirectFds(fd, stream == "stdin")
    }
    Redirect.Inherit -> RedirectFds(-1, -1)
    Redirect.Pipe -> {
        val fds = IntArray(2)
        val piperes = fds.usePinned {
            pipe(it.addressOf(0))
        }
        if (piperes == -1) {
            throw ProcessConfigException(
                "Error opening $stream pipe",
                PosixException.forErrno(posixFunctionName = "pipe()")
            )
        }
        RedirectFds(fds[0], fds[1])
    }
    is Redirect.Read -> {
        val fd = open(file, O_RDONLY)
        if (fd == -1) {
            throw ProcessConfigException(
                "Error opening input file $file for $stream",
                PosixException.forErrno(posixFunctionName = "open()")
            )
        }
        RedirectFds(fd, -1)
    }
    is Redirect.Write -> {
        val fd = open(
            file,
            if (append) O_WRONLY or O_APPEND
            else O_WRONLY
        )
        if (fd == -1) {
            throw ProcessConfigException(
                "Error opening output file $file for $stream",
                PosixException.forErrno(posixFunctionName = "open()")
            )
        }
        RedirectFds(-1, fd)
    }
    Redirect.Stdout -> error("Redirect.Stdout must be handled separately.")
}

@OptIn(ExperimentalIoApi::class)
actual class Process actual constructor(args: ProcessArguments)  {

    private val task = NSTask()

    actual val args: ProcessArguments = args.freeze()


    internal val stdoutFd: Int
    internal val stderrFd: Int
    private val stdinFd: Int

    init {
        var stdout: RedirectFds? = null
        var stderr: RedirectFds? = null
        var stdin: RedirectFds? = null
        try {
            stdout = args.stdout.openFds("stdout")
            stderr = if (args.stderr == Redirect.Stdout)
                RedirectFds(-1, stdout.writeFd)
            else
                args.stderr.openFds("stderr")
            stdin = args.stdin.openFds("sdtin")


            @Suppress("UNCHECKED_CAST")
            task.environment = args.environment as? Map<Any?, *>
            task.setLaunchPath(args.arguments.firstOrNull())
            task.arguments = args.arguments.drop(1)
            task.launch()

            stdoutFd = stdout.readFd
            stderrFd = stderr.readFd
            stdinFd = stdin.writeFd

            stdout.writeFd.closeFd()
            stderr.writeFd.closeFd()
            stdin.readFd.closeFd()
        } catch (e: Throwable) {
            stdout?.readFd?.closeFd()
            stdout?.writeFd?.closeFd()
            if (args.stderr != Redirect.Stdout) {
                stderr?.readFd?.closeFd()
                stderr?.writeFd?.closeFd()
            }
            stdin?.readFd?.closeFd()
            stdin?.writeFd?.closeFd()
            throw e
        }
    }

    actual val isAlive: Boolean
        get() = task.isRunning()

    actual val exitCode: Int?
        get() = task.terminationStatus

    actual fun waitFor(): Int {
        task.waitUntilExit()
        return task.terminationStatus
    }

    @ExperimentalTime
    actual fun waitFor(timeout: Duration): Int? = runBlocking {
        delay(timeout)
        task.terminationStatus
    }

    actual val stdin: Output? by lazy {
        if (stdinFd != -1) Output(stdinFd)
        else null
    }
    actual val stdout: Input? by lazy {
        if (stdoutFd != -1) Input(stdoutFd)
        else null
    }
    actual val stderr: Input? by lazy {
        if (stderrFd != -1) Input(stderrFd)
        else null
    }

    actual fun terminate() {
        task.terminate()
    }

    actual fun kill() {
        task.terminate()
    }
}
