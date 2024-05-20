plugins {
    id("internal-lib")
    id("kotlinx-serialization")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ktpack-internal:models"))
                implementation(project(":ktpack-internal:platform"))
                implementation(libs.coroutines.core)
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
                //implementation(libs.kotlinx.datetime)
                implementation(libs.ksubprocess)
                implementation(libs.ktor.client.core)
                implementation(libs.kotlin.io)
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
