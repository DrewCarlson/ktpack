package ktpack.util

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem


actual fun getHomePath(): Path? {
    return System.getProperty("user.home")
        ?.run(::Path)
        ?.run(SystemFileSystem::resolve)
}

actual fun getEnv(key: String): String? {
    return System.getenv(key)
}

actual fun exitProcess(code: Int): Nothing = kotlin.system.exitProcess(code)
