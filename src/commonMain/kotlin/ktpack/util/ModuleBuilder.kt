package ktpack.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import ksubprocess.*
import ktfio.*
import ktpack.configuration.*

sealed class ArtifactResult {
    data class Success(
        val artifactPath: String,
        val compilationDuration: Double,
        val outputText: String,
    ) : ArtifactResult()

    data class ProcessError(
        val exitCode: Int,
        val message: String?,
    ) : ArtifactResult()

    object NoArtifactFound : ArtifactResult()
}

class ModuleBuilder(
    private val module: ModuleConf,
    private val debug: Boolean,
) {

    val srcFolder = File("src")
    val srcFiles by lazy { srcFolder.listFiles().toList() }
    val mainSource: File?
        get() = srcFiles.find { it.getName() == "main.kt" }

    val binFolder = File("src${filePathSeparator}bin")
    val otherBins: List<File>
        get() = if (binFolder.exists()) binFolder.listFiles().toList() else emptyList()

    // Collect other non-bin source files
    val sourceFiles: List<File>
        get() {
            val otherBinPaths = otherBins.map(File::getAbsolutePath)
            return srcFolder
                .walkTopDown()
                .filter { file ->
                    val absolutePath = file.getAbsolutePath()
                    file.getName().endsWith(".kt") &&
                        absolutePath != mainSource?.getAbsolutePath() &&
                        !otherBinPaths.contains(absolutePath)
                }
                .toList()
        }

    suspend fun buildBin(releaseMode: Boolean, targetBin: String): ArtifactResult = Dispatchers.Default.invoke {
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        val mainSource = mainSource

        val outDir = File("out")
        if (!outDir.exists()) outDir.mkdirs()

        val selectedBinFile = if (targetBin == module.name) {
            checkNotNull(mainSource)
        } else {
            otherBins.find { it.nameWithoutExtension == targetBin }
        } ?: return@invoke ArtifactResult.NoArtifactFound
        val outputPath = "${outDir.getAbsolutePath()}${filePathSeparator}${module.name}"
        val (result, duration) = measureSeconds {
            compileBin(selectedBinFile, sourceFiles, releaseMode, outputPath)
        }
        return@invoke if (result.exitCode == 0) {
            ArtifactResult.Success(
                artifactPath = "$outputPath.$EXE_EXTENSION",
                compilationDuration = duration,
                outputText = listOf(result.output, result.errors).joinToString("\n"),
            )
        } else {
            ArtifactResult.ProcessError(result.exitCode, result.errors.ifBlank { result.output })
        }
    }

    fun buildLib(releaseMode: Boolean): List<ArtifactResult> {
        return emptyList()
    }

    suspend fun buildAllBins(releaseMode: Boolean): List<ArtifactResult> {
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        val mainSource = mainSource

        val artifactPaths = mutableListOf<ArtifactResult>()

        val outDir = File("out")
        if (!outDir.exists()) outDir.mkdirs()

        if (mainSource?.exists() == true) {
            val outputPath = "${outDir.getAbsolutePath()}${filePathSeparator}${module.name}"
            val (result, duration) = measureSeconds {
                compileBin(mainSource, sourceFiles, releaseMode, outputPath)
            }
            if (result.exitCode == 0) {
                artifactPaths.add(
                    ArtifactResult.Success(
                        artifactPath = "$outputPath.$EXE_EXTENSION",
                        compilationDuration = duration,
                        outputText = listOf(result.output, result.errors).joinToString("\n"),
                    )
                )
            }
        }
        otherBins.forEach { otherBin ->
            val otherBinName = otherBin.nameWithoutExtension
            val otherOutput = "${outDir.getAbsolutePath()}/$otherBinName"
            val (result, duration) = measureSeconds {
                compileBin(otherBin, sourceFiles, releaseMode, otherOutput)
            }
            if (result.exitCode == 0) {
                artifactPaths.add(
                    ArtifactResult.Success(
                        artifactPath = "$otherOutput.$EXE_EXTENSION",
                        compilationDuration = duration,
                        outputText = listOf(result.output, result.errors).joinToString("\n"),
                    )
                )
            }
        }

        return artifactPaths.toList()
    }

    suspend fun buildAll(releaseMode: Boolean): List<ArtifactResult> {
        return buildLib(releaseMode) + buildAllBins(releaseMode)
    }
}

private suspend fun compileBin(
    mainSource: File,
    sourceFiles: List<File>,
    releaseMode: Boolean,
    outputPath: String,
): CommunicateResult = exec {
    arg(findKotlincNative("1.7.10"))
    arg(mainSource.getAbsolutePath())
    sourceFiles.forEach { file ->
        arg(file.getAbsolutePath())
    }
    arg("-verbose")

    stderr = Redirect.Pipe
    stdout = Redirect.Pipe

    // kotlinc (jvm) only: output folder/ZIP/Jar path
    // arg("-p")
    // arg("path")

    // kotlinc-native only: binary output path+name
    arg("-o")
    arg(outputPath)

    if (releaseMode) {
        arg("-opt") // kotlinc-native only: enable compilation optimizations
    } else {
        arg("-g") // kotlinc-native only: emit debug info
    }
    println(arguments)
}

private suspend fun compileLib(
    sourceFiles: List<File>,
    releaseMode: Boolean,
    outputPath: String,
): CommunicateResult = exec {
    arg(findKotlincNative("1.7.10"))

    sourceFiles.forEach { file ->
        arg(file.getAbsolutePath())
    }

    // kotlinc (jvm) only: output folder/ZIP/Jar path
    // arg("-p")
    // arg("path")

    // kotlinc-native only: binary output path+name
    arg("-o")
    arg(outputPath)

    arg("-p")
    arg("library")

    if (releaseMode) {
        arg("-opt") // kotlinc-native only: enable compilation optimizations
    } else {
        arg("-g") // kotlinc-native only: emit debug info
    }
}
