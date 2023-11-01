package ktpack.toolchains


interface InstallDetails {
    val path: String
    val isActive: Boolean
    val version: String
}
