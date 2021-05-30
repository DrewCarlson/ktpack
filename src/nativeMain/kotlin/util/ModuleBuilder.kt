package ktpack.util

import com.github.xfel.ksubprocess.*
import ktpack.configuration.*
import me.archinamon.fileio.*

sealed class ArtifactResult {
    data class Success(
        val artifactPath: String,
        val compilationDuration: Double,
    ) : ArtifactResult()

    data class Error(
        val message: String?,
    ) : ArtifactResult()
}

class ModuleBuilder(
    private val module: ModuleConf,
) {

    val srcFolder = File("src")
    val srcFiles by lazy { srcFolder.listFiles().toList() }
    val mainSource: File?
        get() = srcFiles.find { it.getName() == "main.kt" }

    val binFolder = File("src/bin")
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

    fun buildBin(releaseMode: Boolean, targetBin: String): ArtifactResult? {
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        val mainSource = mainSource

        val outDir = File("out/")
        if (!outDir.exists()) outDir.mkdirs()

        val baseOutPath = "out"
        val selectedBinFile = if (targetBin == module.name) {
            checkNotNull(mainSource)
        } else {
            otherBins.find { it.nameWithoutExtension == targetBin }
        }
        return if (selectedBinFile == null) {
            null
        } else {
            val outputPath = "$baseOutPath/${module.name}"
            lateinit var result: CommunicateResult
            val duration = measureSeconds {
                result = compileBin(selectedBinFile, sourceFiles, releaseMode, outputPath)
            }
            if (result.exitCode == 0) {
                ArtifactResult.Success(
                    artifactPath = "${outputPath}.kexe",
                    compilationDuration = duration,
                )
            } else {
                ArtifactResult.Error(null)
            }
        }
    }

    fun buildLib(releaseMode: Boolean): List<ArtifactResult> {
        return emptyList()
    }

    fun buildAllBins(releaseMode: Boolean): List<ArtifactResult> {
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        val mainSource = mainSource

        val artifactPaths = mutableListOf<ArtifactResult>()

        val outDir = File("out/")
        if (!outDir.exists()) outDir.mkdirs()

        val baseOutPath = "out"
        if (mainSource?.exists() == true) {
            val outputPath = "$baseOutPath/${module.name}"
            lateinit var result: CommunicateResult
            val duration = measureSeconds {
                result = compileBin(mainSource, sourceFiles, releaseMode, outputPath)
            }
            if (result.exitCode == 0) {
                artifactPaths.add(
                    ArtifactResult.Success(
                        artifactPath = "${outputPath}.kexe",
                        compilationDuration = duration,
                    )
                )
            }
        }
        otherBins.forEach { otherBin ->
            val otherBinName = otherBin.nameWithoutExtension
            val otherOutputPath = "$baseOutPath/$otherBinName"
            lateinit var result: CommunicateResult
            val duration = measureSeconds {
                result = compileBin(otherBin, sourceFiles, releaseMode, otherOutputPath)
            }
            if (result.exitCode == 0) {
                artifactPaths.add(
                    ArtifactResult.Success(
                        artifactPath = "${otherOutputPath}.kexe",
                        compilationDuration = duration,
                    )
                )
            }
        }

        return artifactPaths.toList()
    }

    fun buildAll(releaseMode: Boolean): List<ArtifactResult> {
        return emptyList()
    }
}

private fun compileBin(
    mainSource: File,
    sourceFiles: List<File>,
    releaseMode: Boolean,
    outputPath: String,
): CommunicateResult = exec {
    // TODO: actual kotlinc lookup and default selection behavior kotlinc/kotlinc-native
    arg("/usr/local/bin/kotlinc-native")
    //arg("%homepath%/.konan/kotlin-native-prebuilt-windows-1.5.10/bin/kotlinc-native.bat")

    arg(mainSource.getAbsolutePath())
    sourceFiles.forEach { file ->
        arg(file.getAbsolutePath())
    }

    // kotlinc (jvm) only: output folder/ZIP/Jar path
    //arg("-p")
    //arg("path")

    // kotlinc-native only: binary output path+name
    arg("-o")
    arg(outputPath)

    if (releaseMode) {
        arg("-opt") // kotlinc-native only: enable compilation optimizations
    } else {
        arg("-g") // kotlinc-native only: emit debug info
    }
}

private fun compileLib(
    sourceFiles: List<File>,
    releaseMode: Boolean,
    outputPath: String,
): CommunicateResult = exec {
    // TODO: actual kotlinc lookup and default selection behavior kotlinc/kotlinc-native
    arg("/usr/local/bin/kotlinc-native")
    //arg("%homepath%/.konan/kotlin-native-prebuilt-windows-1.5.10/bin/kotlinc-native.bat")

    sourceFiles.forEach { file ->
        arg(file.getAbsolutePath())
    }

    // kotlinc (jvm) only: output folder/ZIP/Jar path
    //arg("-p")
    //arg("path")

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
