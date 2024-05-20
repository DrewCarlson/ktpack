package ktpack.util

import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.getenv


actual fun getEnv(key: String): String? {
    return getenv(key)?.toKStringFromUtf8()
}

actual fun exitProcess(code: Int): Nothing = kotlin.system.exitProcess(code)

