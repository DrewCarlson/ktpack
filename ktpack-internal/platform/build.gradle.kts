plugins {
    id("internal-lib")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ktpack-internal:models"))
                implementation(libs.okio)
            }
        }
    }
}
