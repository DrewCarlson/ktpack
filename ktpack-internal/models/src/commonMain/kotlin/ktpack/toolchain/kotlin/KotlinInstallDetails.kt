package ktpack.toolchain.kotlin

import ktpack.toolchain.InstallDetails


data class KotlinInstallDetails(
    val type: CompilerType,
    override val version: String,
    override val path: String,
    override val isActive: Boolean,
) : InstallDetails {

    enum class CompilerType {
        JVM, NATIVE
    }
}
