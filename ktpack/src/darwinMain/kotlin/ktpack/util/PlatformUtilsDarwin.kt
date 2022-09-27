package ktpack.util

import platform.Foundation.NSTemporaryDirectory

actual val tempPath: String = NSTemporaryDirectory()
