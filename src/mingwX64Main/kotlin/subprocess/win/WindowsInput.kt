package ktpack.subprocess.win

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import ktpack.subprocess.close
import platform.windows.HANDLE

@Suppress("FunctionName")
@OptIn(ExperimentalIoApi::class)
fun Input(handle: HANDLE?): Input = WindowsInputForFileHandle(handle)

@OptIn(ExperimentalIoApi::class)
private class WindowsInputForFileHandle(val handle: HANDLE?) : Input() {
    private var closed = false
    override fun closeSource() {
        if (closed) return
        closed = true
        handle.close()
    }

    override fun fill(destination: Memory, offset: Int, length: Int): Int {
        return read(handle, destination, length).toInt()
    }
}
