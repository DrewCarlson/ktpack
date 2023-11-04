package mongoose

expect suspend fun runWebServer(
    httpPort: Int,
    onServerStarted: () -> Unit = {},
    body: MongooseRouting.() -> Unit
)
