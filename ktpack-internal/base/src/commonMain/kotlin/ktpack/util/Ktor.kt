package ktpack.util

import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.io.files.Path

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

suspend fun HttpStatement.downloadInto(
    outputPath: Path,
    bufferSize: Long = DOWNLOAD_BUFFER_SIZE,
): HttpResponse {
    return execute { response ->
        val body = response.bodyAsChannel()
        outputPath.sink().use { sink ->
            while (!body.exhausted()) {
                body.readRemaining(bufferSize).transferTo(sink)
            }
        }
        response
    }
}
