package ktpack.github.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GhRelease(
    val name: String,
    @SerialName("tag_name")
    val tagName: String,
    val prerelease: Boolean,
    val assets: List<GhAsset>,
)

@Serializable
data class GhAsset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url")
    val downloadUrl: String,
)

@Serializable
data class GhTag(
    val name: String,
)
