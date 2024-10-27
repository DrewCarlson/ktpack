plugins {
    id("internal-lib")
    alias(libs.plugins.serialization)
    alias(libs.plugins.binaryCompat)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ktpack-internal:platform"))
                implementation(libs.serialization.core)
                implementation(libs.tomlkt)
            }
        }
    }
}
