package ktpack.util

import com.github.ajalt.mordant.terminal.*
import ktpack.commands.*
import ktpack.configuration.*
import ktpack.subprocess.*
import me.archinamon.fileio.*

class ModuleBuilder(
    private val term: Terminal?,
    private val manifest: ManifestConf,
    private val releaseMode: Boolean,
    private val targetBin: String?,
) {

    fun build(): List<String> {
        val module = manifest.module
        val artifactPaths = mutableListOf<String>()

        val srcFolder = File("src")
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        val srcFiles = srcFolder.listFiles()
        val mainSource = srcFiles.find { it.getName() == "main.kt" }
        val binFolder = File("src/bin")
        val otherBins = if (binFolder.exists()) binFolder.listFiles().toList() else emptyList()
        val otherBinPaths = otherBins.map(File::getAbsolutePath)

        val outDir = File("out/")
        if (!outDir.exists()) outDir.mkdirs()

        // Collect other non-bin source files
        val sourceFiles = srcFolder
            .walkTopDown()
            .filter { file ->
                val absolutePath = file.getAbsolutePath()
                file.getName().endsWith(".kt") &&
                    absolutePath != mainSource?.getAbsolutePath() &&
                    !otherBinPaths.contains(absolutePath)
            }
            .toList()

        term?.println("${success("Compiling")} ${module.name} v${module.version} (${srcFolder.getParent()})")
        val compileDuration = measureSeconds {
            val baseOutPath = "out"
            val buildMain = targetBin.isNullOrBlank() || targetBin == module.name
            if (mainSource?.exists() == true && buildMain) {
                val outputPath = "$baseOutPath/${module.name}"
                val result = compileBin(mainSource, sourceFiles, releaseMode, outputPath)
                if (result.exitCode == 0) {
                    artifactPaths.add("${outputPath}.kexe")
                }
            }
            if (targetBin.isNullOrBlank()) {
                otherBins
            } else {
                listOfNotNull(otherBins.find { it.nameWithoutExtension == targetBin })
            }.forEach { otherBin ->
                val otherBinName = otherBin.nameWithoutExtension
                val otherOutputPath = "$baseOutPath/$otherBinName"
                val result = compileBin(otherBin, sourceFiles, releaseMode, otherOutputPath)
                if (result.exitCode == 0) {
                    artifactPaths.add("${otherOutputPath}.kexe")
                }
            }
        }

        term?.println(buildString {
            append(success("Finished"))
            if (releaseMode) {
                append(" release [optimized] target(s)")
            } else {
                append(" dev [unoptimized + debuginfo] target(s)")
            }
            append(" in ${compileDuration}s")
        })

        return artifactPaths.toList()
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
