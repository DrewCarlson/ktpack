package ktpack.util

import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.getenv

actual val TEMP_PATH: Path by lazy {
    val tempPath: String = (getenv("TMPDIR") ?: getenv("TMP"))?.toKStringFromUtf8() ?: "/tmp"
    tempPath.toPath().apply {
        if (!exists()) {
            check(mkdirs().exists()) { "Failed to create temp directory: ${toString()}" }
        }
    }
}
