plugins {
    id("internal-lib")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.autoHeadResponse)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.coroutines.test)
            }
        }
    }
}
