package ktpack.jdk

import kotlinx.serialization.Serializable

@Serializable
enum class JdkDistribution {
    Zulu,
    Temurin,
    Corretto,
}
