package ktpack.subprocess.win

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import ktpack.subprocess.close
import platform.windows.HANDLE

@Suppress("FunctionName")
@OptIn(ExperimentalIoApi::class)
fun Output(handle: HANDLE?): Output = WindowsOutputForFileHandle(handle)

@OptIn(ExperimentalIoApi::class)
private class WindowsOutputForFileHandle(val handle: HANDLE?) : Output() {
    private var closed = false
    override fun closeDestination() {
        if (closed) return
        closed = true
        handle.close()
    }

    override fun flush(source: Memory, offset: Int, length: Int) {
        write(handle, source, length)
    }
}
