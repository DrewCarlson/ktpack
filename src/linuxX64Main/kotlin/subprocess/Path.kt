package ktpack.subprocess

import platform.posix.F_OK
import platform.posix.X_OK
import platform.posix.access


val executablePaths: List<String> by lazy {
    Environment["PATH"]?.split(':') ?: listOf()
}

fun findExecutable(name: String): String? =
    executablePaths.map { "$it/$name" }.first { access(it, F_OK or X_OK) == 0 }
