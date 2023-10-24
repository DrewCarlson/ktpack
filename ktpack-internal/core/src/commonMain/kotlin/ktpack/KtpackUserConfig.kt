package ktpack

import kotlinx.serialization.Serializable
import ktpack.jdk.JdkDistribution
import ktpack.util.USER_HOME
import okio.Path.Companion.DIRECTORY_SEPARATOR

@Serializable
data class KtpackUserConfig(
    val kotlin: KotlinConfig = KotlinConfig(),
    val jdk: JdkConfig = JdkConfig(),
) {
    @Serializable
    data class KotlinConfig(
        val version: String = Ktpack.KOTLIN_VERSION,
        val rootPath: String = "${USER_HOME}${DIRECTORY_SEPARATOR}.konan",
    )

    @Serializable
    data class JdkConfig(
        val distribution: JdkDistribution = JdkDistribution.Zulu,
        val version: String = "17",
        val rootPath: String = "${USER_HOME}${DIRECTORY_SEPARATOR}.jdks",
    )
}
