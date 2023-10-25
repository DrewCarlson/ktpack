import org.gradle.nativeplatform.platform.internal.*

plugins {
    id("internal-lib")
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":ktpack-internal:platform"))
                implementation(libs.ktfio)
                implementation(libs.ksubprocess)
                implementation(libs.coroutines.core)
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization)
                implementation(libs.kotlinx.datetime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(project(":ktpack-internal:test-utils"))
                implementation(libs.coroutines.test)
            }
        }

        if (!hostOs.isLinux) {
            val windowsX64Main by getting {
                dependencies {
                    implementation(libs.ktor.client.winhttp)
                }
            }
        }

        if (!hostOs.isWindows) {
            val linuxX64Main by getting {
                dependencies {
                    implementation(libs.ktor.client.curl)
                }
            }
        }

        if (hostOs.isMacOsX) {
            val darwinMain by getting {
                dependencies {
                    implementation(libs.ktor.client.darwin)
                }
            }
        }
    }
}
