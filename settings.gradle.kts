rootProject.name = "ktpack"

include(
    ":ktpack-cli",
    ":ktpack-cli-tests",
    ":ktpack-internal:core",
    ":ktpack-internal:compression",
    ":ktpack-internal:dependency-resolver",
    ":ktpack-internal:dokka",
    ":ktpack-internal:git",
    ":ktpack-internal:gradle",
    ":ktpack-internal:maven",
    ":ktpack-internal:models",
    ":ktpack-internal:mongoose",
    ":ktpack-internal:platform",
    ":ktpack-internal:test-utils",
    ":libs:mongoose",
    ":libs:zip",
)

pluginManagement {
    includeBuild("build-logic")
}
