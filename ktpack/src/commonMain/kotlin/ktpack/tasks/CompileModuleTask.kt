package ktpack.tasks

import ktpack.configuration.ModuleConf
import ktpack.configuration.Target
import ktpack.task.BaseTask
import ktpack.util.ArtifactResult
import ktpack.util.ModuleBuilder

class CompileModuleTask(
    /** The module to compile. */
    private val moduleConf: ModuleConf,
    /** The list of targets to compile. */
    // private val targetList: List<Target>,
    /** The bin/lib target name. */
    private val target: String,
    /** Is building in release mode. */
    private val releaseMode: Boolean,
) : BaseTask() {
    override val name: String = "Compile Kotlin"
    override val description: String = "Pass source files to the Kotlin compiler and return the output."

    init {
        doFirst {
            val moduleBuilder = ModuleBuilder(moduleConf, debug = true)
            when (val result = moduleBuilder.buildBin(releaseMode, target, Target.MACOS_ARM64)) {
                is ArtifactResult.Success -> {
                }
                is ArtifactResult.ProcessError -> TODO()
                ArtifactResult.NoArtifactFound -> TODO()
            }
        }
    }
}
