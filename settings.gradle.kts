rootProject.name = "ktpack"

include(
    ":ktpack-cli",
    ":ktpack-cli-tests",
    ":ktpack-models",
    ":ktpack-script",
    ":ktpack-internal:core",
    ":ktpack-internal:compression",
    ":ktpack-internal:dependency-resolver",
    ":ktpack-internal:dokka",
    ":ktpack-internal:git",
    ":ktpack-internal:gradle",
    ":ktpack-internal:maven",
    ":ktpack-internal:test-utils",
    ":libs:mongoose",
    ":libs:zip",
)
