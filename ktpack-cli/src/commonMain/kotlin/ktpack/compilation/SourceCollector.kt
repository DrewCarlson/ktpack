package ktpack.compilation

import co.touchlab.kermit.Logger
import kotlinx.io.files.Path
import ktpack.compilation.ModuleBuilder.BuildType
import ktpack.configuration.KotlinTarget
import ktpack.manifest.OutputToml
import ktpack.util.*


data class CollectedSource(
    val sourceFiles: List<String>,
) {
    val isEmpty = sourceFiles.isEmpty()

    fun merge(collectedSource: CollectedSource): CollectedSource {
        return CollectedSource(
            sourceFiles = sourceFiles + collectedSource.sourceFiles,
        )
    }
}

interface SourceCollector {


    fun collectKotlin(target: KotlinTarget?, buildType: BuildType): CollectedSource
}

class KtpackSourceCollector(
    private val srcFolder: Path,
) : SourceCollector {
    private val logger = Logger.forClass<SourceCollector>()

    fun getDefaultOutput(selectTarget: KotlinTarget? = null): OutputToml {
        val targetList = listOf(selectTarget ?: PlatformUtils.getHostTarget())
        val collectedSource =
            collectKotlin(null, BuildType.BIN)
                .merge(collectKotlin(selectTarget ?: PlatformUtils.getHostTarget(), BuildType.BIN))
        return if (collectedSource.sourceFiles.any { it.endsWith("main.kt") }) {
            OutputToml.BinCommon.Bin(targets = targetList)
        } else {
            OutputToml.Lib(targets = targetList)
        }
    }

    override fun collectKotlin(target: KotlinTarget?, buildType: BuildType): CollectedSource {
        check(srcFolder.isDirectory()) { "Expected directory at $srcFolder" }
        val sourceAliases = when(target) {
            null -> listOf("common")
            // JVM builds multiplatform binaries in one-shot, so collect common sources as well
            KotlinTarget.JVM -> target.aliases + "common"
            else -> target.aliases
        }
        val testAliases = if (buildType == BuildType.TEST) {
            sourceAliases.map { "${it}Test" }
        } else {
            emptyList()
        }
        val allTargetKotlinRoots = (sourceAliases + testAliases)
            .map { alias -> Path(srcFolder, alias, "kotlin") }
        val targetKotlinRoots = allTargetKotlinRoots.filter { it.exists() }

        logger.d {
            val pathStrings = allTargetKotlinRoots.joinToString("\n") {
                val exists = if (targetKotlinRoots.contains(it)) "*" else "!"
                "[$exists] $it"
            }
            "Searching Kotlin source folders [${target ?: "common"}]:\n${pathStrings}"
        }

        val sourceFiles = targetKotlinRoots
            .flatMap { it.listRecursively() }
            .filter { it.name.endsWith(".kt") }
            .map { it.toString() }

        val sources = CollectedSource(sourceFiles = sourceFiles)
        logger.d { "Collected sources:\n${sources.sourceFiles.joinToString("\n")}".trim() }
        return sources
    }
}
