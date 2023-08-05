package ktpack

import kotlinx.serialization.Serializable
import ktfio.filePathSeparator
import ktpack.commands.jdk.JdkDistribution
import ktpack.util.USER_HOME

@Serializable
data class KtpackUserConfig(
    val kotlin: KotlinConfig = KotlinConfig(),
    val jdk: JdkConfig = JdkConfig(),
) {
    @Serializable
    data class KotlinConfig(
        val version: String = Ktpack.KOTLIN_VERSION,
        val rootPath: String = "${USER_HOME}$filePathSeparator.konan",
    )

    @Serializable
    data class JdkConfig(
        val distribution: JdkDistribution = JdkDistribution.Zulu,
        val version: String = "17",
        val rootPath: String = "${USER_HOME}$filePathSeparator.jdks",
    )
}
