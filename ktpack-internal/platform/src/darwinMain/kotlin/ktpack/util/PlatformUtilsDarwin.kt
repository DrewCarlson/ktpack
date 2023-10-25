package ktpack.util

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

actual val TEMP_PATH: Path by lazy {
    NSTemporaryDirectory().toPath().apply {
        FileSystem.SYSTEM.createDirectories(this)
        check(FileSystem.SYSTEM.exists(this)) {
            "Failed to create temp directory: $this"
        }
    }
}

