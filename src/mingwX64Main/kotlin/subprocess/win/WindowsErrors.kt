package ktpack.subprocess.win

import io.ktor.utils.io.core.*
import kotlinx.cinterop.*
import platform.windows.*

private fun MAKELANGID(p: Int, s: Int) = ((s shl 10) or p).toUInt()

@OptIn(ExperimentalIoApi::class)
class WindowsException(val errorCode: DWORD, message: String) : Exception(message) {

    companion object {
        fun fromLastError(
            errorCode: DWORD = GetLastError(),
            functionName: String?
        ): WindowsException = memScoped {
            val msgBufHolder = alloc<LPWSTRVar>()

            FormatMessageW(
                (FORMAT_MESSAGE_ALLOCATE_BUFFER or
                        FORMAT_MESSAGE_FROM_SYSTEM or
                        FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
                NULL,
                errorCode,
                MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                interpretCPointer(msgBufHolder.rawPtr),
                0,
                null
            )

            val sysMsg = msgBufHolder.value?.toKString()

            val sysMsgWithCode = if (sysMsg == null) {
                "error #${errorCode.toString(16)}"
            } else {
                "error #${errorCode.toString(16)}: $sysMsg"
            }

            val message =
                if (functionName != null) "$functionName failed with $sysMsgWithCode"
                else sysMsgWithCode

            WindowsException(errorCode, message)
        }
    }
}


