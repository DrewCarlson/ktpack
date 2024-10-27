plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    System.getenv("GITHUB_REF_NAME")
        ?.takeIf { it.startsWith("v") }
        ?.let { version = it.removePrefix("v") }
}
