package ktpack.util

import kotlinx.cinterop.*
import platform.posix.*

actual fun getHomePath(): String? {
    return getenv("HOME")?.toKString()
        ?: getpwuid(getuid())?.pointed?.pw_dir?.toKString()
}

actual val workingDirectory: String by lazy {
    memScoped {
        val bufLen = 250
        allocArray<ByteVar>(bufLen).apply { getcwd(this, bufLen.convert()) }.toKString()
    }
}
