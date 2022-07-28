package ktpack.configuration

import kotlinx.serialization.Serializable

@Serializable
class DependencyContainer(
    val targets: List<Target>,
    val dependencies: List<KtpackDependency>,
)
