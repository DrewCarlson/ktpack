package ktpack.subprocess.win

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.internal.*
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.*
import platform.posix.SSIZE_MAX
import platform.posix.size_t
import platform.posix.ssize_t
import platform.windows.*

internal const val SZERO: ssize_t = 0
internal const val ZERO: size_t = 0u

@OptIn(DangerousInternalIoApi::class, ExperimentalIoApi::class)
fun read(handle: HANDLE?, destination: Memory, length: Int): ssize_t {
    var bytesRead: ssize_t = 0

    val size = minOf(
        DWORD.MAX_VALUE.toULong(),
        SSIZE_MAX.toULong(),
        length.toULong()
    ).convert<DWORD>()

    val result = memScoped {
        val rVar = alloc<DWORDVar>()
        if (ReadFile(handle, destination.pointer, size, rVar.ptr, null) == 0) {
            val ec = GetLastError()
            // handle some errors in a special way
            when (ec.toInt()) {
                ERROR_BROKEN_PIPE -> {
                    // pipe got closed, essentially an EOF
                    return@memScoped 0u
                }
            }
            throw IOException(
                "IO operation failed due to windows error",
                WindowsException.fromLastError(ec, functionName = "ReadFile")
            )
        }
        rVar.value
    }

    bytesRead = result.convert()

    // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
    // however the returned value could be -1 so clamp it
    result.convert<Int>().coerceAtLeast(0)

    return bytesRead
}

@ExperimentalIoApi
fun write(handle: HANDLE?, destination: Memory, length: Int): ssize_t {
    var written: ssize_t = 0

    val result = memScoped {
        val rVar = alloc<DWORDVar>()
        if (WriteFile(handle, destination.pointer, length.convert(), rVar.ptr, null) == 0) {
            throw IOException(
                "IO operation failed due to windows error",
                WindowsException.fromLastError(functionName = "WriteFile")
            )
        }
        rVar.value
    }

    written = result.convert()

    // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
    // however the returned value could be -1 so clamp it
    result.convert<Int>().coerceAtLeast(0)

    return written
}
