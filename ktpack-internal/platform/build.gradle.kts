import org.gradle.nativeplatform.platform.internal.*

plugins {
    id("internal-lib")
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":ktpack-models"))
                implementation(libs.okio)
            }
        }

        val commonTest by getting {
            dependencies {
            }
        }
    }
}
