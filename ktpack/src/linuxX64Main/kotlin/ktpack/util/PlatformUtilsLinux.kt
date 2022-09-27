package ktpack.util

import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.getenv

actual val tempPath: String = (getenv("TMPDIR") ?: getenv("TMP"))?.toKStringFromUtf8() ?: "/tmp"
