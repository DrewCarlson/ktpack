package mongoose

import co.touchlab.kermit.Logger
import kotlinx.cinterop.*
import kotlinx.coroutines.*

private data class FuncData(
    val routing: MongooseRouting,
    val logger: Logger = Logger.withTag("Mongoose"),
)

actual suspend fun runWebServer(
    httpPort: Int,
    onServerStarted: () -> Unit,
    body: MongooseRouting.() -> Unit,
): Unit = memScoped {
    val routing = MongooseRouting().apply(body)
    val arg = StableRef.create(FuncData(routing))
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
    val (routing, logger) = checkNotNull(dataRef).get()
    when (ev.toUInt()) {
        MG_EV_OPEN -> logger.d { "MG_EV_OPEN" }
        MG_EV_CLOSE -> logger.d { "MG_EV_CLOSE" }
        MG_EV_ERROR -> {
            val hm = checkNotNull(evData?.reinterpret<mg_str>()).pointed
            logger.e { "Error event: ${hm.toKString()}" }
        }

        MG_EV_HTTP_MSG -> {
            val hm = checkNotNull(evData?.reinterpret<mg_http_message>()).pointed
            handleHttpMessage(logger, hm, con, routing, evData)
        }
    }
}

private fun handleHttpMessage(
    logger: Logger,
    hm: mg_http_message,
    con: CPointer<mg_connection>?,
    routing: MongooseRouting,
    evData: COpaquePointer?,
): Unit = memScoped {
    logger.d { "Handling request: ${hm.method.toKString()} @ ${hm.uri.toKString()}" }
    val handler = routing.routes
        .firstNotNullOfOrNull { (path, handler) ->
            if (mg_http_match_uri(hm.ptr, path)) {
                handler
            } else {
                null
            }
        } ?: routing.getGlobal() ?: return@memScoped

    val route = MongooseRoute().apply(handler)
    when (val bodyType = route.bodyType) {
        is MongooseRoute.BodyType.File -> {
            if (bodyType.directory) {
                logger.d { "Serving from resources dir: ${bodyType.path}" }
                val opts = alloc<mg_http_serve_opts> {
                    root_dir = bodyType.path.cstr.ptr
                }
                mg_http_serve_dir(con, evData?.reinterpret(), opts.ptr)
            } else {
                logger.d { "Serving artifact file ${bodyType.path}" }
                val opts = alloc<mg_http_serve_opts> {
                    mime_types = "js=application/javascript,svg=image/svg+xml".cstr.ptr
                }
                mg_http_serve_file(con, hm.ptr, bodyType.path, opts.ptr)
            }
        }

        is MongooseRoute.BodyType.Text -> {
            logger.d { "Serving text response" }
            mg_http_reply(
                con,
                200,
                "Content-Type: ${route.contentType}\r\n",
                bodyType.body,
            )
        }

        null -> error("Route must invoke a response method.")
    }
}
