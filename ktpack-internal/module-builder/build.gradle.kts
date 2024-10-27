plugins {
    id("internal-lib")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ktpack-internal:platform"))
                implementation(project(":ktpack-internal:models"))
                implementation(project(":ktpack-internal:manifest-loader"))
                implementation(project(":ktpack-internal:dependency-resolver"))
                implementation(project(":ktpack-internal:toolchains"))
                implementation(libs.ksubprocess)
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
