package ktpack.util

import kotlinx.cinterop.toKStringFromUtf8
import okio.FileSystem
import platform.posix.getenv


actual fun getEnv(key: String): String? {
    return getenv(key)?.toKStringFromUtf8()
}

actual fun exitProcess(code: Int): Nothing = kotlin.system.exitProcess(-1)

actual val SystemFs = FileSystem.SYSTEM
