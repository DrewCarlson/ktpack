rootProject.name = "ktpack"

include(
    ":ktpack-cli",
    ":ktpack-cli-tests",
    ":ktpack-internal:base",
    ":ktpack-internal:compression",
    ":ktpack-internal:dependency-resolver",
    ":ktpack-internal:dokka",
    ":ktpack-internal:git",
    ":ktpack-internal:github",
    ":ktpack-internal:gradle",
    ":ktpack-internal:manifest-loader",
    ":ktpack-internal:maven",
    ":ktpack-internal:models",
    ":ktpack-internal:module-builder",
    ":ktpack-internal:platform",
    ":ktpack-internal:test-utils",
    ":ktpack-internal:toolchains",
    ":ktpack-internal:webserver",
    ":libs:zip",
)

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    includeBuild("build-logic")
}
