import org.gradle.jvm.tasks.Jar

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
        implementation(kotlin("scripting-jvm-host"))
        compileOnly(kotlin("scripting-jvm"))
        implementation(kotlin("scripting-dependencies"))
        implementation(kotlin("scripting-dependencies-maven-all"))
        implementation(kotlin("script-runtime"))
        api(project(":ktpack-internal:models"))
        implementation(libs.coroutines.core)
        implementation(libs.serialization.json)
        implementation(libs.kotlinpoet)
        implementation(libs.ktoml.core)

        testImplementation(kotlin("test"))
        testImplementation(kotlin("test-junit"))
    }
}

tasks.withType<org.gradle.api.tasks.bundling.Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("ktpack-script.jar")
    // TODO: The following doesn't seem to be consistent with 1.9, needs investigation
    // stdlib will be provided by kotlinc
    //dependencies { exclude(dependency("org.jetbrains.kotlin:.*:.*")) }
}

spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
    }
}
