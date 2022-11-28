package ktpack.util

import kotlinx.cinterop.toKStringFromUtf8
import ktfio.File
import platform.posix.getenv

actual val TEMP_DIR: File by lazy {
    val tempPath: String = (getenv("TMPDIR") ?: getenv("TMP"))?.toKStringFromUtf8() ?: "/tmp"
    File(tempPath).also { file ->
        if (!file.exists()) {
            check(file.mkdirs()) { "Failed to create temp directory: ${file.getAbsolutePath()}" }
        }
    }
}
