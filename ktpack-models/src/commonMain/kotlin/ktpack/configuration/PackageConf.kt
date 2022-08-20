package ktpack.configuration

import kotlinx.serialization.Serializable

@Serializable
data class PackageConf(
    val module: ModuleConf,
)
