package ktpack.util

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.nio.file.FileSystems


actual fun getHomePath(): String? {
    return System.getProperty("user.home")
}

actual val workingDirectory: String =
    FileSystems.getDefault()
        .getPath("")
        .toAbsolutePath()
        .toString()

actual val TEMP_PATH: Path =
    System.getProperty("java.io.tmpdir").toPath()


actual fun getEnv(key: String): String? {
    return System.getenv(key)
}

actual fun exitProcess(code: Int): Nothing = kotlin.system.exitProcess(-1)

actual val SystemFs = FileSystem.SYSTEM
