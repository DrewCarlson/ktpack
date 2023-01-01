@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}
