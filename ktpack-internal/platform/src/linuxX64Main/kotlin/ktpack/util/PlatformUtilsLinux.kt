package ktpack.util

import okio.Path
import okio.Path.Companion.toPath
import kotlinx.cinterop.toKStringFromUtf8
import okio.FileSystem
import platform.posix.getenv

actual val TEMP_PATH: Path by lazy {
    val tempPath: String = (getenv("TMPDIR") ?: getenv("TMP"))?.toKStringFromUtf8() ?: "/tmp"
    tempPath.toPath().apply {
        FileSystem.SYSTEM.createDirectories(this)
        check(FileSystem.SYSTEM.exists(this)) {
            "Failed to create temp directory: $this"
        }
    }
}
