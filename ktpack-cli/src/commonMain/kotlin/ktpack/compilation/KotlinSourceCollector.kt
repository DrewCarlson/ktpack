package ktpack.compilation

import co.touchlab.kermit.Logger
import ktfio.*
import ktpack.compilation.ModuleBuilder.BuildType
import ktpack.configuration.KotlinTarget
import ktpack.util.exists
import ktpack.util.isDirectory
import okio.Path


data class CollectedSource(
    val sourceFiles: List<String>,
    val mainFile: String?,
    val binFiles: List<String>,
) {
    val hasLibFile = mainFile != null || binFiles.isNotEmpty()
    val isEmpty = mainFile == null && sourceFiles.isEmpty() && binFiles.isEmpty()

    fun merge(collectedSource: CollectedSource): CollectedSource {
        return CollectedSource(
            sourceFiles = sourceFiles + collectedSource.sourceFiles,
            mainFile = collectedSource.mainFile ?: mainFile,
            binFiles = binFiles + collectedSource.binFiles
        )
    }
}

class KotlinSourceCollector(
    private val srcFolder: Path,
) {
    private val logger = Logger.withTag(KotlinSourceCollector::class.simpleName.orEmpty())

    fun collect(target: KotlinTarget?, buildType: BuildType): CollectedSource {
        check(srcFolder.isDirectory()) { "Expected directory at $srcFolder" }
        val (mainFileName, secondaryDir) = when (buildType) {
            BuildType.BIN -> "main.kt" to "bin"
            BuildType.LIB -> "lib.kt" to "lib"
            BuildType.TEST -> "" to ""
        }
        val sourceAliases = target?.aliases ?: listOf("common")
        val testAliases = if (buildType == BuildType.TEST) {
            sourceAliases.map { "${it}Test" }
        } else {
            emptyList()
        }
        val allTargetKotlinRoots = (sourceAliases + testAliases)
            .map { alias -> (srcFolder / alias / "kotlin") }
        val targetKotlinRoots = allTargetKotlinRoots.filter { it.exists() }

        logger.d {
            val pathStrings = allTargetKotlinRoots.joinToString("\n") {
                val exists = if (targetKotlinRoots.contains(it)) "*" else "!"
                "[$exists] $it"
            }
            "Searching Kotlin source folders:\n${pathStrings}"
        }

        val sourceFiles = targetKotlinRoots.flatMap { targetKotlinRoot ->
            val targetFolder = File(targetKotlinRoot.toString())
            targetFolder.walkTopDown()
                .onEnter { folder ->
                    if (folder.getParentFileUnsafe() == targetFolder) {
                        folder.getName() != secondaryDir
                    } else {
                        true
                    }
                }
                .filter { file ->
                    val fileName = file.getName()
                    fileName.endsWith(".kt") && fileName != mainFileName
                }
                .map(File::getAbsolutePath)
                .toList()
        }
        val mainFile = targetKotlinRoots.firstNotNullOfOrNull { kotlinRoot ->
            (kotlinRoot / mainFileName).takeIf(Path::exists)
        }
        val binFiles = targetKotlinRoots.flatMap { kotlinRoot ->
            val binRoot = File(kotlinRoot.toString()).nestedFile(secondaryDir)
            if (binRoot.exists()) {
                binRoot.walk()
                    .drop(1) // Ignore the parent folder
                    .map(File::getAbsolutePath)
                    .toList()
            } else {
                emptyList()
            }
        }

        val sources = CollectedSource(
            sourceFiles = sourceFiles,
            mainFile = mainFile?.toString(),
            binFiles = binFiles,
        )
        logger.d { "Collected sources:\n${sources.sourceFiles.joinToString("\n")}".trim() }
        logger.d { "Collected bin files:\n${sources.binFiles.joinToString("\n")}".trim() }
        logger.d { "Collected main file:\n${sources.mainFile.orEmpty()}".trim() }
        return sources
    }
}
