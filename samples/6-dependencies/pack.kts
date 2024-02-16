module("hello_world") {
    version = "0.0.1"
    kotlinVersion = "1.9.22"
    description = "A sample that uses multiple KMP libraries."
    dependencies {
        maven(libs.kermit)
        maven(libs.coingecko)
        maven(libs.ktor)
    }
    dependencies(JS_BROWSER, JS_NODE) {
        maven(libs.ktor.js)
    }
    dependencies(JVM) {
        maven(libs.ktor.cio)
    }
    dependencies(LINUX_X64) {
        maven(libs.ktor.curl)
    }
    dependencies(MACOS_X64) {
        maven(libs.ktor.darwin)
    }
    dependencies(MINGW_X64) {
        maven(libs.ktor.winhttp)
    }
}
