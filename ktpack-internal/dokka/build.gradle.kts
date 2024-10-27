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
                implementation(project(":ktpack-internal:dependency-resolver"))
                implementation(libs.ksubprocess)
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
