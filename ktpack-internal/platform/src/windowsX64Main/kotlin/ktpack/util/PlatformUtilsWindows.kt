package ktpack.util

import kotlinx.cinterop.*
import okio.FileSystem
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
    val tempPath = checkNotNull(getenv("temp")).toKStringFromUtf8().toPath()
    if (!FileSystem.SYSTEM.exists(tempPath)) {
        FileSystem.SYSTEM.createDirectories(tempPath, mustCreate = false)
        check(FileSystem.SYSTEM.exists(tempPath)) {
            "Failed to create temp directory: $tempPath"
        }
    }
    tempPath
}
