package ktpack.util

import kotlinx.cinterop.*
import platform.posix.*
import okio.Path
import okio.Path.Companion.toPath

actual fun getHomePath(): String? {
    return getenv("HOME")?.toKString()
        ?: getpwuid(getuid())?.pointed?.pw_dir?.toKString()
}

actual val workingDirectory: Path by lazy {
    memScoped {
        val bufLen = 250
        allocArray<ByteVar>(bufLen).apply { getcwd(this, bufLen.convert()) }.toKString().toPath()
    }
}
