@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

sourceSets {
    dependencies {
        implementation(libs.serialization.json)
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("ktpack-manifest.jar")
    minimize()
    // stdlib will be provided by kotlinc
    dependencies { exclude(dependency("org.jetbrains.kotlin:.*:.*")) }
}