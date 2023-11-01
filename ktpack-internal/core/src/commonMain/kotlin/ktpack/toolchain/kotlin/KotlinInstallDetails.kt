package ktpack.toolchain.kotlin

import ktpack.toolchains.InstallDetails


data class KotlinInstallDetails(
    val type: KotlincInstalls.CompilerType,
    override val version: String,
    override val path: String,
    override val isActive: Boolean,
) : InstallDetails
