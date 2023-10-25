plugins {
    id("internal-lib")
    alias(libs.plugins.serialization)
    alias(libs.plugins.binaryCompat)
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.serialization.core)
            }
        }
    }
}
