import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("internal-lib")
    alias(libs.plugins.serialization)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

kotlin {
    configure(targets) {
        if (this is KotlinNativeTarget) {
            binaries {
                executable {
                    baseName = "ktpack"
                    entryPoint("ktpack.main")
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ktpack-internal:compression"))
                implementation(project(":ktpack-internal:dependency-resolver"))
                implementation(project(":ktpack-internal:dokka"))
                implementation(project(":ktpack-internal:git"))
                implementation(project(":ktpack-internal:github"))
                implementation(project(":ktpack-internal:gradle"))
                implementation(project(":ktpack-internal:manifest-loader"))
                implementation(project(":ktpack-internal:maven"))
                implementation(project(":ktpack-internal:webserver"))
                implementation(project(":ktpack-internal:models"))
                implementation(project(":ktpack-internal:module-builder"))
                implementation(project(":ktpack-internal:platform"))
                implementation(project(":ktpack-internal:toolchains"))
                implementation(libs.ksubprocess)
                implementation(libs.mordant)
                implementation(libs.clikt)
                implementation(libs.semver)
                implementation(libs.ktor.serialization)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.contentNegotiation)
            }
        }

        commonTest {
            dependencies {
                implementation(project(":ktpack-internal:test-utils"))
                implementation(libs.coroutines.test)
            }
        }
    }
}
