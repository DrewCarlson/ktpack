@file:OptIn(ExperimentalIoApi::class)

package ktpack.subprocess

import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.errors.*
import io.ktor.utils.io.streams.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.freeze
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


private fun MemScope.toCStrVector(data: List<String>): CArrayPointer<CPointerVar<ByteVar>> {
    val res = allocArray<CPointerVar<ByteVar>>(data.size + 1)
    for (i in data.indices) {
        res[i] = data[i].cstr.ptr
    }
    res[data.size] = null
    return res
}


actual class Process actual constructor(args: ProcessArguments) {

    actual val args = args.freeze()

    private var terminated = false
    private var _exitStatus = -1

    private val childPid: pid_t

    internal val stdoutFd: Int
    internal val stderrFd: Int
    private val stdinFd: Int

    init {
        var executable = args.arguments[0]

        if ('/' !in executable) {
            executable = findExecutable(executable)
                ?: throw ProcessConfigException("Unable to find executable '$executable' on PATH")
        }

        args.workingDirectory?.let {
            val dir = opendir(it) ?: throw ProcessConfigException("Working directory '$it' cannot be used!")
            closedir(dir)
        }

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

            val pid = memScoped {
                // covnvert c lists
                val arguments = toCStrVector(args.arguments)
                val env = args.environment?.let { toCStrVector(it.map { e -> "${e.key}=${e.value}" }) }

                forkAndRun(
                    executable,
                    arguments,
                    args.workingDirectory,
                    env,
                    stdout.writeFd,
                    stderr.writeFd,
                    stdin.readFd,
                    stdout.readFd,
                    stderr.readFd,
                    stdin.writeFd
                )
            }
            if (pid == -1) {
                throw ProcessException(
                    "Error staring subprocess",
                    PosixException.forErrno(posixFunctionName = "fork()")
                )
            }
            childPid = pid

            stdoutFd = stdout.readFd
            stderrFd = stderr.readFd
            stdinFd = stdin.writeFd

            stdout.writeFd.closeFd()
            stderr.writeFd.closeFd()
            stdin.readFd.closeFd()
        } catch (t: Throwable) {
            stdout?.readFd?.closeFd()
            stdout?.writeFd?.closeFd()
            if (args.stderr != Redirect.Stdout) {
                stderr?.readFd?.closeFd()
                stderr?.writeFd?.closeFd()
            }
            stdin?.readFd?.closeFd()
            stdin?.writeFd?.closeFd()
            throw t
        }
    }

    private fun cleanup() {
        stdoutFd.closeFd()
        stderrFd.closeFd()
        stdinFd.closeFd()
    }

    private fun checkState(block: Boolean = false) {
        if (terminated) return
        var options = 0
        if (!block) {
            options = options or WNOHANG
        }
        memScoped {
            val info = alloc<siginfo_t>()
            val res = waitid(idtype_t.P_PID, childPid.convert(), info.ptr, options or WEXITED)
            if (res == -1) {
                throw ProcessException(
                    "Error querying process state",
                    PosixException.forErrno(posixFunctionName = "waitpid()")
                )
            }
            when (info.si_code) {
                CLD_EXITED, CLD_KILLED, CLD_DUMPED -> {
                    terminated = true
                    _exitStatus = info._sifields._sigchld.si_status
                    cleanup()
                }
            }
        }
    }

    actual val isAlive: Boolean
        get() {
            checkState()
            return !terminated
        }
    actual val exitCode: Int?
        get() {
            checkState()
            return if (terminated) _exitStatus else null
        }

    actual fun waitFor(): Int {
        while (!terminated) {
            checkState(true)
        }
        return _exitStatus
    }

    @ExperimentalTime
    actual fun waitFor(timeout: Duration): Int? {
        require(timeout.isPositive()) { "Timeout must be positive!" }
        val clk = TimeSource.Monotonic
        val deadline = clk.markNow() + timeout
        while (true) {
            checkState(false)
            if (terminated) return _exitStatus
            if (deadline.hasPassedNow()) return null
            memScoped {
                val ts = alloc<timespec>()
                ts.tv_nsec = 50 * 1000
                nanosleep(ts.ptr, ts.ptr)
            }
        }
    }

    actual fun terminate() {
        sendSignal(SIGTERM)
    }

    actual fun kill() {
        sendSignal(SIGKILL)
    }

    fun sendSignal(signal: Int) {
        if (terminated) return
        if (kill(childPid, signal) != 0) {
            throw ProcessException(
                "Error terminating process",
                PosixException.forErrno(posixFunctionName = "kill()")
            )
        }
    }

    actual val stdin: Output? by lazy {
        if (stdinFd != -1) Output(stdinFd) else null
    }

    actual val stdout: Input? by lazy {
        if (stdoutFd != -1) Input(stdoutFd) else null
    }

    actual val stderr: Input? by lazy {
        if (stderrFd != -1) Input(stderrFd) else null
    }
}

