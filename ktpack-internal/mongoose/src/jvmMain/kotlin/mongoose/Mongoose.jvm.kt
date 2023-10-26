package mongoose


actual suspend fun runWebServer(
    httpPort: Int,
    data: HttpAccessHandlerData,
    onServerStarted: () -> Unit
) {
    error("JVM web server not implemented yet")
}
