package ktpack.configuration

import kotlinx.serialization.Serializable

@Serializable
data class ManifestConf(
    val module: ModuleConf,
)
