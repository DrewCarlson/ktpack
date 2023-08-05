package ktpack.configuration

import kotlinx.serialization.Serializable

@Serializable
data class KtpackConf(
    val module: ModuleConf,
)
