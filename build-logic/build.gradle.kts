plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlinpoet)
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-conventions:0.9.0")
}

repositories {
    gradlePluginPortal()
}
