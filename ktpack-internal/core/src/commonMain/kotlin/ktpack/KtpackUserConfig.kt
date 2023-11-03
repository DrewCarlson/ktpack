package ktpack

import kotlinx.serialization.Serializable
import ktpack.toolchain.jdk.JdkDistribution
import ktpack.util.KTPACK_ROOT
import ktpack.util.USER_HOME
import ktpack.util.pathFrom

@Serializable
data class KtpackUserConfig(
    val kotlin: KotlinConfig = KotlinConfig(),
    val jdk: JdkConfig = JdkConfig(),
    val nodejs: NodejsConfig = NodejsConfig(),
) {
    @Serializable
    data class KotlinConfig(
        val version: String = Ktpack.KOTLIN_VERSION,
        val rootPath: String = pathFrom(USER_HOME, ".konan").toString(),
    )

    @Serializable
    data class JdkConfig(
        val distribution: JdkDistribution = JdkDistribution.Zulu,
        val version: String = "17",
        val rootPath: String = pathFrom(USER_HOME, ".jdks").toString(),
    )

    @Serializable
    data class NodejsConfig(
        val version: String = "20.9.0",
        val rootPath: String = (KTPACK_ROOT / "nodejs").toString(),
    )
}
