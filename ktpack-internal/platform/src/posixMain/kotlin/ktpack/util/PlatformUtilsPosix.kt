package ktpack.util

import kotlinx.cinterop.*
import platform.posix.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual fun getHomePath(): Path? {
    val result = getenv("HOME")?.toKString()
        ?: getpwuid(getuid())?.pointed?.pw_dir?.toKString()
    return result?.run(::Path)?.run(SystemFileSystem::resolve)
}
