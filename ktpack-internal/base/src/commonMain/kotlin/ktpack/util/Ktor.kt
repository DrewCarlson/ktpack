package ktpack.util

import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

suspend fun HttpStatement.downloadInto(
    outputPath: Path,
    bufferSize: Long = DOWNLOAD_BUFFER_SIZE,
): HttpResponse {
    return execute { response ->
        val body = response.bodyAsChannel()
        val sink = SystemFileSystem.sink(outputPath, append = true)
        val bufferedSink = sink.buffered()
        try {
            while (!body.isClosedForRead) {
                val packet = body.readRemaining(bufferSize)
                while (!packet.exhausted()) {
                    bufferedSink.write(packet.readByteArray())
                }
            }
        } finally {
            bufferedSink.close()
            sink.close()
        }
        response
    }
}
