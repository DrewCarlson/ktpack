package ktpack.util

import io.ktor.client.statement.*
import io.ktor.utils.io.core.*
import okio.FileSystem
import okio.Path
import okio.buffer

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

suspend fun HttpStatement.downloadInto(
    outputPath: Path,
    bufferSize: Long = DOWNLOAD_BUFFER_SIZE,
    fileSystem: FileSystem = SystemFs
): HttpResponse {
    return execute { response ->
        val body = response.bodyAsChannel()
        val sink = fileSystem.appendingSink(outputPath)
        val bufferedSink = sink.buffer()
        try {
            while (!body.isClosedForRead) {
                val packet = body.readRemaining(bufferSize)
                while (packet.isNotEmpty) {
                    bufferedSink.write(packet.readBytes())
                }
            }
        } finally {
            bufferedSink.close()
            sink.close()
        }
        response
    }
}
