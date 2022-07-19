package ktpack.util

import kotlinx.cinterop.*
import platform.posix.*

actual fun getHomePath(): String? {
    return getenv("HOME")?.toKString()
        ?: getpwuid(getuid())?.pointed?.pw_dir?.toKString()
}
