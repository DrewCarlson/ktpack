plugins {
    id("internal-lib")
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ktpack-internal:platform"))
                implementation(project(":ktpack-internal:compression"))
                implementation(project(":ktpack-internal:dokka"))
                implementation(project(":ktpack-internal:models"))
                implementation(project(":ktpack-internal:github"))
                implementation(libs.ksubprocess)
                implementation(libs.cryptohash)
                implementation(libs.semver)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization)
            }
        }

        commonTest {
            dependencies {
                implementation(project(":ktpack-internal:test-utils"))
                implementation(libs.coroutines.test)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }

        windowsMain {
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }

        linuxMain {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }

        appleMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
