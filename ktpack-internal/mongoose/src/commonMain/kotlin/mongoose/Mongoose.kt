package mongoose

data class HttpAccessHandlerData(
    val moduleName: String,
    val kotlinVersion: String,
    val artifactPath: String,
    val artifactName: String,
)

expect suspend fun runWebServer(
    httpPort: Int,
    data: HttpAccessHandlerData,
    onServerStarted: () -> Unit
)
