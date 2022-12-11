import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.spotless) apply false
}

yarn.lockFileDirectory = file("gradle/kotlin-js-store")

subprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}
