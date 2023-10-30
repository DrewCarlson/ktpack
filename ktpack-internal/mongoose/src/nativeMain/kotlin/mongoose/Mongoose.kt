package mongoose

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import kotlinx.coroutines.*

private data class FuncData(
    val handlerData: HttpAccessHandlerData,
    val logger: Logger = Logger.withTag("Mongoose"),
)

actual suspend fun runWebServer(
    httpPort: Int,
    data: HttpAccessHandlerData,
    onServerStarted: () -> Unit,
): Unit = memScoped {
    val arg = StableRef.create(FuncData(data))
    val manager = alloc<mg_mgr>()
    // mg_log_set(MG_LL_VERBOSE.toInt())
    mg_mgr_init(manager.ptr)
    checkNotNull(mg_http_listen(manager.ptr, "http://0.0.0.0:$httpPort", httpFunc, arg.asCPointer())) {
        "Failed to start server at port $httpPort"
    }
    onServerStarted()

    defer { mg_mgr_free(manager.ptr) }
    while (true) {
        mg_mgr_poll(manager.ptr, 1000)
        yield()
    }
}

private fun mg_str.toKString(): String? {
    return ptr?.readBytes(len.toInt())?.toKString()
}

private val httpFunc: mg_event_handler_t = staticCFunction { con, ev, evData, fnData ->
    val dataRef = fnData?.asStableRef<FuncData>()
    val (data, logger) = checkNotNull(dataRef).get()
    when (ev.toUInt()) {
        MG_EV_OPEN -> logger.d { "MG_EV_OPEN" }
        MG_EV_CLOSE -> logger.d { "MG_EV_CLOSE" }
        MG_EV_ERROR -> {
            val hm = checkNotNull(evData?.reinterpret<mg_str>()).pointed
            logger.e { "Error event: ${hm.toKString()}" }
        }

        MG_EV_HTTP_MSG -> {
            val hm = checkNotNull(evData?.reinterpret<mg_http_message>()).pointed
            handleHttpMessage(logger, hm, con, data, evData)
        }
    }
}

private fun handleHttpMessage(
    logger: Logger,
    hm: mg_http_message,
    con: CPointer<mg_connection>?,
    data: HttpAccessHandlerData,
    evData: COpaquePointer?,
): Unit = memScoped {
    logger.d { "Handling request: ${hm.method.toKString()} @ ${hm.uri.toKString()}" }
    if (mg_http_match_uri(hm.ptr, "/") || mg_http_match_uri(hm.ptr, "/index.html")) {
        logger.d { "Serving index.html file" }
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
        logger.d { "Serving artifact file" }
        val opts = alloc<mg_http_serve_opts> {
            mime_types = "js=application/javascript".cstr.ptr
        }
        mg_http_serve_file(con, hm.ptr, data.artifactPath, opts.ptr)
    } else {
        logger.d { "Serving from resources dir" }
        val opts = alloc<mg_http_serve_opts> {
            root_dir = ".".cstr.ptr
        }
        mg_http_serve_dir(con, evData?.reinterpret(), opts.ptr)
    }
}

// TODO: Link kotlin.js from the stdlib-js jar
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
