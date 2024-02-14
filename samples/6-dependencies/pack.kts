module("hello_world") {
    version = "0.0.1"
    kotlinVersion = "1.9.22"
    description = "A sample that uses multiple KMP libraries."
    val ktorVersion = "2.3.8"
    dependencies {
        maven("co.touchlab:kermit:2.0.3")
        maven("org.drewcarlson:coingecko:1.0.0-beta02")
    }
    dependencies(JS_BROWSER, JS_NODE) {
        maven("io.ktor:ktor-client-js:$ktorVersion")
    }
    dependencies(JVM) {
        maven("io.ktor:ktor-client-cio:$ktorVersion")
    }
    dependencies(LINUX_X64) {
        maven("io.ktor:ktor-client-curl:$ktorVersion")
    }
    dependencies(MACOS_X64) {
        maven("io.ktor:ktor-client-darwin:$ktorVersion")
    }
    dependencies(MINGW_X64) {
        maven("io.ktor:ktor-client-winhttp:$ktorVersion")
    }
}
