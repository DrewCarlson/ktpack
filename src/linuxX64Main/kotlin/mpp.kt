package ktpack

import kotlinx.cinterop.*
import platform.posix.*

actual fun readFile(path: String): String {
    val file = requireNotNull(fopen(path, "r")) { "Failed to open $path" }
    return StringBuilder().apply {
        try {
            memScoped {
                val readBufferLength = 64 * 1024
                val buffer = allocArray<ByteVar>(readBufferLength)
                var line: String?
                do {
                    line = fgets(buffer, readBufferLength, file)?.toKString()?.also(::append)
                } while (line != null)
            }
        } finally {
            fclose(file)
        }
    }.toString()
}
