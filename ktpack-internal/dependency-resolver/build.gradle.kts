plugins {
    id("internal-lib")
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ktpack-internal:maven"))
                implementation(project(":ktpack-internal:gradle"))
                implementation(project(":ktpack-internal:models"))
                implementation(project(":ktpack-internal:platform"))
                implementation(libs.semver)
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
