@Suppress("DSL_SCOPE_VIOLATION")
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
        compileOnly(kotlin("scripting-jvm"))
        compileOnly(kotlin("script-runtime"))
        api(project(":ktpack-models"))
        implementation(libs.serialization.json)

        testImplementation(kotlin("test"))
        testImplementation(kotlin("test-junit"))
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("ktpack-script.jar")
    // stdlib will be provided by kotlinc
    dependencies { exclude(dependency("org.jetbrains.kotlin:.*:.*")) }
}

spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
            //.setUseExperimental(true)
            .editorConfigOverride(
                mapOf(
                    "disabled_rules" to "no-wildcard-imports,no-unused-imports,trailing-comma,filename"
                )
            )
    }
}
