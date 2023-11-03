package ktpack.toolchain.jdk

import ktpack.toolchain.InstallDetails


data class JdkInstallDetails(
    val distribution: JdkDistribution,
    val intellijManifest: String?,
    override val version: String,
    override val path: String,
    override val isActive: Boolean,
) : InstallDetails {
    val isIntellijInstall: Boolean = !intellijManifest.isNullOrBlank()
}
