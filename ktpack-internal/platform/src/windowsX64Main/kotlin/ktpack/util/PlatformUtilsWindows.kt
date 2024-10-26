package ktpack.util

import kotlinx.cinterop.*
import kotlinx.io.files.Path
import platform.posix.*
import platform.posix.NULL
import platform.windows.*

actual fun getHomePath(): Path? {
    val result =  getenv("userprofile")?.toKString() ?: memScoped {
        val path = allocArray<WCHARVar>(MAX_PATH)
        if (SHGetFolderPathW(null, CSIDL_PROFILE, NULL, 0u, path) == 0) {
            path.toKString()
        } else {
            null
        }
    }
    return result?.run(::Path)?.resolve()
}

