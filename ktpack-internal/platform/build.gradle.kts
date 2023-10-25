plugins {
    id("internal-lib")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ktpack-models"))
                implementation(libs.okio)
            }
        }
    }
}
