package mongoose

import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

suspend fun runWebServer(
    httpPort: Int,
    onServerStarted: () -> Unit = {},
    body: MongooseRouting.() -> Unit,
) {
    withContext(Dispatchers.IO) {
        embeddedServer(CIO, port = httpPort) {
            val builder = MongooseRouting().apply(body)
            routing {
                builder.routes.forEach { (path, handler) ->
                    get(path) {
                        val route = MongooseRoute().apply(handler)

                        when (val bodyType = route.bodyType) {
                            is MongooseRoute.BodyType.File -> {
                                if (bodyType.directory) {
                                    call.respondText(
                                        contentType = ContentType.Text.Html,
                                        provider = { renderDirectory(Path(path)) },
                                    )
                                } else {
                                    call.respondText(
                                        contentType = route.contentType,
                                        provider = {
                                            SystemFileSystem.source(Path(bodyType.path))
                                                .use { source ->
                                                    source.buffered().use { buf ->
                                                        buf.readString()
                                                    }
                                                }
                                        },
                                    )
                                }
                            }

                            is MongooseRoute.BodyType.Text -> {
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

    return """
<body>
    <h1>${dir}</h1>
    <hr/>
    <pre>
<a href="../">../</a>
${
        sortedFiles.joinToString(separator = "\n") {
            """<a href="${it.name}${if (SystemFileSystem.metadataOrNull(it)?.isDirectory == true) "/" else ""}">${it.name}</a>"""
        }
    }
    </pre>
</body>
    """.trimIndent()
}

