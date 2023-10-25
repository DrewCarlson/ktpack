import org.gradle.nativeplatform.platform.internal.*

plugins {
    id("internal-lib")
    alias(libs.plugins.serialization)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":ktpack-internal:core"))
                implementation(libs.coroutines.core)
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(project(":ktpack-internal:test-utils"))
                implementation(libs.coroutines.test)
            }
        }
    }
}
