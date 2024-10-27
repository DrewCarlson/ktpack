plugins {
    id("internal-lib")
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlin.io)
            }
        }
    }
}
