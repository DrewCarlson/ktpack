package ktpack.tasks

import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf
import ktpack.task.BaseTask

class CompileModuleTask(
    /** The module to compile. */
    private val moduleConf: ModuleConf,
    /** The target to compile for. */
    private val target: KotlinTarget,
    /** The bin/lib name to compile. */
    private val artifactName: String,
    /** Is building in release mode. */
    private val releaseMode: Boolean,
) : BaseTask() {
    override val name: String = buildString {
        append("compileKotlin")
        append(moduleConf.name.asTaskPart())
        append(artifactName.asTaskPart())
        append(if (releaseMode) "Release" else "Debug")
    }
    override val description: String = "Pass source files to the Kotlin compiler and return the output."

    init {
        doFirst {
            /*val moduleBuilder = ModuleBuilder(moduleConf, )
            when (val result = moduleBuilder.buildBin(releaseMode, artifactName, target)) {
                is ArtifactResult.Success -> {
                }

                is ArtifactResult.ProcessError -> TODO()
                ArtifactResult.NoArtifactFound -> TODO()
            }*/
        }
    }
}

fun String.asTaskPart(): String = lowercase().replaceFirstChar(Char::uppercase)
