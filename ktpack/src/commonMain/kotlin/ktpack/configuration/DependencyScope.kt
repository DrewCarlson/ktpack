package ktpack.configuration

import kotlinx.serialization.Serializable


@Serializable
enum class DependencyScope {
    IMPLEMENTATION,
    API,
    TEST,
    COMPILE,
}