package ktpack.configuration

import kotlinx.serialization.Serializable

@Serializable
class DependencyContainer(
    val targets: List<KotlinTarget>,
    val dependencies: List<DependencyConf>,
)

@Serializable
data class ExcludeDependency(
    val path: String,
    val version: String?
) {

}

@Serializable
class VersionConstraint(

)
