package webserver

import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import ktpack.util.*

suspend fun runWebServer(
    httpPort: Int,
    onServerStarted: () -> Unit = {},
    body: WebserverRouting.() -> Unit,
) {
    withContext(Dispatchers.IO) {
        embeddedServer(CIO, port = httpPort) {
            val builder = WebserverRouting().apply(body)
            routing {
                builder.routes.forEach { (httpPath, handler) ->
                    get(httpPath) {
                        val route = WebserverRoute().apply(handler)

                        when (val bodyType = route.bodyType) {
                            is WebserverRoute.BodyType.File -> {
                                val basePath = Path(bodyType.path)
                                if (bodyType.directory) {
                                    val requestedPath = Path(basePath, call.request.uri.trimStart('/'))
                                    if (requestedPath.isDirectory()) {
                                        val indexHtml = Path(requestedPath, "index.html")
                                        if (indexHtml.exists()) {
                                            call.respondBytes(
                                                contentType = ContentType.Text.Html,
                                                provider = { indexHtml.readByteArray() },
                                            )
                                        } else {
                                            call.respondText(
                                                contentType = ContentType.Text.Html,
                                                provider = { renderDirectory(requestedPath) },
                                            )
                                        }
                                    } else {
                                        if (requestedPath.exists()) {
                                            call.respondBytes(
                                                contentType = ContentType.defaultForFilePath(requestedPath.toString()),
                                                provider = { requestedPath.readByteArray() },
                                            )
                                        } else {
                                            call.respond(HttpStatusCode.NotFound)
                                        }
                                    }
                                } else {
                                    call.respondBytes(
                                        contentType = route.contentType,
                                        provider = { basePath.readByteArray() },
                                    )
                                }
                            }

                            is WebserverRoute.BodyType.Text -> {
                                call.respondText(
                                    contentType = route.contentType,
                                    provider = { bodyType.body },
                                )
                            }

                            null -> error("Body type not defined")
                        }
                    }
                }
            }
            onServerStarted()
        }.start(wait = true)
    }
}

private fun renderDirectory(dir: Path): String {
    val files = SystemFileSystem.list(dir).toList()
    val sortedFiles = files.sortedBy { it.name }
    val fileLinks = sortedFiles.joinToString(separator = "\n") {
        """<a href="./${it.name}${if (it.isDirectory()) "/" else ""}">${it.name}</a>"""
    }
    return """
    <body>
        <h1>${dir}</h1>
        <hr/>
        <pre>
<a href="../">../</a>
$fileLinks
        </pre>
    </body>
    """.trimIndent()
}

