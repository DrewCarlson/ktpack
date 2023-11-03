package ktpack.toolchain.nodejs

import ktpack.toolchain.InstallDetails

data class NodejsInstallDetails(
    override val version: String,
    override val path: String,
    override val isActive: Boolean,
) : InstallDetails
