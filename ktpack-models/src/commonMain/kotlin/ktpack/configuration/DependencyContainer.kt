package ktpack.configuration

import kotlinx.serialization.Serializable

@Serializable
class DependencyContainer(
    val targets: List<KotlinTarget>,
    val dependencies: List<DependencyConf>
)
