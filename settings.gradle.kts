rootProject.name = "ktpack"

enableFeaturePreview("VERSION_CATALOGS")

includeBuild("external/native-file-io") {
    dependencySubstitution {
        substitute(module("me.archinamon:fileio"))
            .with(project(":"))
    }
}

includeBuild("external/mordant") {
    dependencySubstitution {
        substitute(module("com.github.ajalt.mordant:mordant"))
            .with(project(":mordant"))
    }
}
