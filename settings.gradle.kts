rootProject.name = "ktpack"

include(
    ":ktpack-cli",
    ":ktpack-cli-tests",
    ":ktpack-internal:core",
    ":ktpack-internal:compression",
    ":ktpack-internal:dependency-resolver",
    ":ktpack-internal:dependency-resolver-public",
    ":ktpack-internal:dokka",
    ":ktpack-internal:git",
    ":ktpack-internal:gradle",
    ":ktpack-internal:maven",
    ":ktpack-internal:models",
    ":ktpack-internal:platform",
    ":ktpack-internal:test-utils",
    ":ktpack-internal:webserver",
    ":libs:zip",
)

pluginManagement {
    includeBuild("build-logic")
}
