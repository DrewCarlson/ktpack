plugins {
    id("internal-lib")
}

kotlin {
    sourceSets {
        commonMain {
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
                //implementation(libs.kotlinx.datetime)
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
