import org.gradle.nativeplatform.platform.internal.*

plugins {
    id("internal-lib")
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":ktpack-internal:core"))
                implementation(project(":ktpack-internal:maven"))
                implementation(project(":ktpack-internal:gradle"))
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
