package ktpack.toolchain


sealed class ToolchainInstallProgress {
    abstract val completed: Int

    data class Started(
        val downloadUrl: String,
        override val completed: Int = 0,
    ) : ToolchainInstallProgress()

    data class Download(override val completed: Int) : ToolchainInstallProgress()
    data class Extract(override val completed: Int) : ToolchainInstallProgress()
}

