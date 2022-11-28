package ktpack.util

import ktfio.File
import platform.Foundation.NSTemporaryDirectory

actual val TEMP_DIR: File by lazy {
    File(NSTemporaryDirectory()).also { file ->
        if (!file.exists()) {
            check(file.mkdirs()) { "Failed to create temp directory: ${file.getAbsolutePath()}" }
        }
    }
}
