package webserver

import io.ktor.http.*


typealias WebserverRouteHandler = WebserverRoute.() -> Unit

class WebserverRouting {

    internal val routes = mutableMapOf<String, WebserverRouteHandler>()

    internal fun getGlobal(): WebserverRouteHandler? {
        return routes.firstNotNullOfOrNull { (key, handler) ->
            if (key.isBlank()) {
                handler
            } else {
                null
            }
        }
    }

    fun route(path: String, handler: WebserverRouteHandler) {
        require(!routes.contains(path)) { "A route is already defined for path '$path'" }
        routes[path] = handler
    }

    fun indexRoute(handler: WebserverRouteHandler) {
        require(!routes.contains("/") && !routes.contains("/")) {
            "A route is already defined for path `/` or `/index.html`"
        }
        routes["/"] = handler
        routes["/index.html"] = handler
    }
}

class WebserverRoute {
    internal var contentType: ContentType? = null
    internal var bodyType: BodyType? = null

    fun contentType(contentType: ContentType) {
        this.contentType = contentType
    }

    fun respondBody(text: String) {
        bodyType = BodyType.Text(text)
    }

    fun respondFile(path: String) {
        bodyType = BodyType.File(path, directory = false)
    }

    fun respondDirectory(path: String) {
        bodyType = BodyType.File(path, directory = true)
    }

    internal sealed class BodyType {
        class Text(
            val body: String,
        ) : BodyType()

        class File(
            val path: String,
            val directory: Boolean,
        ) : BodyType()
    }
}
