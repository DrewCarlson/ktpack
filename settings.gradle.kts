rootProject.name = "ktpack"

include(
    ":ktpack-cli",
    ":ktpack-cli-tests",
    ":ktpack-internal:compression",
    ":ktpack-internal:base",
    ":ktpack-internal:dependency-resolver",
    ":ktpack-internal:dokka",
    ":ktpack-internal:git",
    ":ktpack-internal:github",
    ":ktpack-internal:gradle",
    ":ktpack-internal:maven",
    ":ktpack-internal:models",
    ":ktpack-internal:platform",
    ":ktpack-internal:test-utils",
    ":ktpack-internal:toolchains",
    ":ktpack-internal:webserver",
    ":libs:zip",
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    includeBuild("build-logic")
}
