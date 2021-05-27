package ktpack.subprocess

import io.ktor.utils.io.core.*
import ktpack.subprocess.win.WindowsException
import kotlinx.cinterop.convert
import platform.windows.*

// close a windows handle and optionally report errors
@OptIn(ExperimentalIoApi::class)
internal fun HANDLE?.close(ignoreErrors: Boolean = false) {
    if (this != INVALID_HANDLE_VALUE) {
        if (CloseHandle(this) == 0 && !ignoreErrors) {
            val ec = GetLastError()
            if (ec.convert<Int>() != ERROR_INVALID_HANDLE) {
                throw ProcessException(
                    "Error closing handle",
                    WindowsException.fromLastError(ec, "CloseHandle")
                )
            }
        }
    }
}
