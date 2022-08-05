package ktpack

import kotlinx.serialization.Serializable
import ktfio.filePathSeparator
import ktpack.commands.jdk.JdkDistribution
import ktpack.util.USER_HOME

@Serializable
data class KtpackUserConfig(
    var kotlinVersion: String = Ktpack.KOTLIN_VERSION,
    var kotlincJvmRootPath: String? = null,
    var kotlincNativeRootPath: String? = null,
    var jdkDistribution: JdkDistribution = JdkDistribution.Zulu,
    var jdkVersion: String = "17",
    var jdkRootPath: String = "${USER_HOME}$filePathSeparator.jdks",
)
