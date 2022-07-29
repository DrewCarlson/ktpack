package ktpack.util

import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.NULL
import platform.windows.*

actual fun getHomePath(): String? {
    return getenv("userprofile")?.toKString() ?: memScoped {
        val path = allocArray<WCHARVar>(MAX_PATH)
        if (SHGetFolderPathW(null, CSIDL_PROFILE, NULL, 0, path) == 0) {
            path.toKString()
        } else null
    }
}

actual val workingDirectory: String by lazy {
    memScoped {
        allocArray<ByteVar>(MAX_PATH).apply { getcwd(this, MAX_PATH) }.toKString()
    }
}
