plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
    alias(libs.plugins.binaryCompat)
    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow)
}

kotlin {
    explicitApi()
}

sourceSets {
    dependencies {
        compileOnly(kotlin("scripting-jvm-host"))
        compileOnly(kotlin("scripting-jvm"))
        compileOnly(kotlin("script-runtime"))
        compileOnly(kotlin("stdlib"))
        api(project(":ktpack-internal:models"))
        implementation(libs.serialization.json)
        implementation(libs.kotlinpoet)
        implementation(libs.ktoml.core)

        testImplementation(kotlin("test"))
        testImplementation(kotlin("test-junit"))
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("ktpack-script.jar")
    // TODO: The following doesn't seem to be consistent with 1.9, needs investigation
    // stdlib will be provided by kotlinc
    dependencies { exclude(dependency("org.jetbrains.kotlin:.*:.*")) }
}

spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
    }
}
