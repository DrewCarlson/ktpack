package ktpack.util

import platform.Foundation.NSTemporaryDirectory

actual val TEMP_PATH: Path by lazy {
    NSTemporaryDirectory().toPath().apply {
        if (!exists()) {
            check(mkdirs().exists()) { "Failed to create temp directory: ${toString()}" }
        }
    }
}

