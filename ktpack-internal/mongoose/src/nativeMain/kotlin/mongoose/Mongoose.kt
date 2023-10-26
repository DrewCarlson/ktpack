package mongoose

import kotlinx.cinterop.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield

actual suspend fun runWebServer(
    httpPort: Int,
    data: HttpAccessHandlerData,
    onServerStarted: () -> Unit
) = memScoped {
    val arg = StableRef.create(data)
    val manager = alloc<mg_mgr>()
    mg_mgr_init(manager.ptr)
    mg_http_listen(manager.ptr, "http://0.0.0.0:$httpPort", httpFunc, arg.asCPointer())
    onServerStarted()

    defer { mg_mgr_free(manager.ptr) }
    while (currentCoroutineContext().isActive) {
        mg_mgr_poll(manager.ptr, mg_millis().convert())
        yield()
    }
}

private val httpFunc: mg_event_handler_t = staticCFunction { con, ev, evData, fnData ->
    if (ev.toUInt() == MG_EV_HTTP_MSG) {
        memScoped {
            val dataRef = fnData?.asStableRef<HttpAccessHandlerData>()
            defer { dataRef?.dispose() }
            val data = checkNotNull(dataRef).get()
            val hm = checkNotNull(evData?.reinterpret<mg_http_message>()).pointed
            if (mg_http_match_uri(hm.ptr, "/") || mg_http_match_uri(hm.ptr, "/index.html")) {
                mg_http_reply(
                    con,
                    200,
                    "Content-Type: text/html\r\n",
                    DEFAULT_HTML,
                    data.moduleName,
                    data.kotlinVersion,
                    data.artifactName,
                )
            } else if (mg_http_match_uri(hm.ptr, "/${data.artifactName}")) {
                val opts = alloc<mg_http_serve_opts> {
                    mime_types = "js=application/javascript".cstr.ptr
                }
                mg_http_serve_file(con, hm.ptr, data.artifactPath, opts.ptr)
            } else {
                val opts = alloc<mg_http_serve_opts> {
                    root_dir = ".".cstr.ptr
                }
                mg_http_serve_dir(con, evData?.reinterpret(), opts.ptr)
            }
        }
    }
}

// TODO: Link kotlin.js locally
private val DEFAULT_HTML =
    """|<!DOCTYPE html>
       |<html>
       |<head>
       |    <meta charset=UTF-8>
       |    <title>%s</title>
       |    <script defer="defer" src="https://cdnjs.cloudflare.com/ajax/libs/kotlin/%s/kotlin.min.js" type="application/javascript"></script>
       |    <script defer="defer" src="%s" type="application/javascript"></script>
       |</head>
       |<body></body>
       |</html>
    """.trimMargin()
