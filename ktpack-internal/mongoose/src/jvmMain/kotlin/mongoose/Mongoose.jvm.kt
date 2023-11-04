package mongoose


actual suspend fun runWebServer(
    httpPort: Int,
    onServerStarted: () -> Unit,
    body: MongooseRouting.() -> Unit
) {
    error("JVM web server not implemented yet")
}
