package ktpack.util

import kotlinx.cinterop.*
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.*
import platform.posix.NULL
import platform.windows.*

actual fun getHomePath(): String? {
    return getenv("userprofile")?.toKString() ?: memScoped {
        val path = allocArray<WCHARVar>(MAX_PATH)
        if (SHGetFolderPathW(null, CSIDL_PROFILE, NULL, 0u, path) == 0) {
            path.toKString()
        } else {
            null
        }
    }
}

actual val workingDirectory: String by lazy {
    memScoped {
        allocArray<ByteVar>(MAX_PATH).apply { getcwd(this, MAX_PATH) }.toKString()
    }
}

actual val TEMP_PATH: Path by lazy {
    val tempPath: String = checkNotNull(getenv("temp")).toKStringFromUtf8()
    tempPath.toPath().apply {
        if (!exists()) {
            check(mkdirs().exists()) { "Failed to create temp directory: ${toString()}" }
        }
    }
}
