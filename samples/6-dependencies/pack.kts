module("hello_world") {
    version = "0.0.1"
    kotlinVersion = "1.9.10"
    description = "A sample that produces a library from multiple files."
    dependencies {
        maven("co.touchlab:kermit:2.0.0-RC3")
        maven("io.ktor:ktor-client-core:2.2.4")
        maven("org.drewcarlson:coingecko:1.0.0-beta02")
    }
    dependencies(LINUX_X64) {
        maven("io.ktor:ktor-client-curl:2.2.4")
    }
    dependencies(MACOS_X64) {
        maven("io.ktor:ktor-client-darwin:2.2.4")
    }
    dependencies(MINGW_X64) {
        maven("io.ktor:ktor-client-winhttp:2.2.4")
    }
}
