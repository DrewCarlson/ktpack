package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.terminal.*
import ktpack.*
import ktpack.subprocess.*
import ktpack.util.*
import me.archinamon.fileio.*
import kotlin.math.*
import kotlin.time.*
import kotlin.time.DurationUnit.*

class BuildCommand(private val term: Terminal) : CliktCommand(
    help = "Compile packages and dependencies.",
) {

    private val releaseMode by option("--release")
        .help("Build artifacts in release mode, with optimizations")
        .flag(default = false)

    private val targetBin by option("--bin")
        .help("Build only the specified binary")

    private val libOnly by option("--lib")
        .help("Build only this package's library")
        .flag(default = false)

    private val binsOnly by option("--bins")
        .help("Build all binaries")
        .flag(default = false)

    override fun run() {
        val manifest = loadManifest(MANIFEST_NAME)
        val module = manifest.module

        val srcFolder = File("src")
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        val srcFiles = srcFolder.listFiles()
        val mainSource = srcFiles.find { it.getName() == "main.kt" }
        val binFolder = File("src/bin")
        val otherBins = if (binFolder.exists()) binFolder.listFiles() else emptyArray()
        val otherBinPaths = otherBins.map(File::getAbsolutePath)

        val outDir = File("out/")
        if (!outDir.exists()) outDir.mkdirs()

        // Collect other non-bin source files
        val sourceFiles = srcFolder
            .walkTopDown()
            .filter { file ->
                val absolutePath = file.getAbsolutePath()
                file.getName().endsWith(".kt") &&
                    absolutePath != mainSource.getAbsolutePath() &&
                    !otherBinPaths.contains(absolutePath)
            }
            .toList()

        term.println("${success("Compiling")} ${module.name} v${module.version} (${srcFolder.getParent()})")
        val compileDuration = measureSeconds {
            val baseOutPath = "out"
            if (mainSource?.exists() == true) {
                val mainBinName = mainSource.nameWithoutExtension
                val outputPath = "$baseOutPath/$mainBinName"
                compileBin(mainSource, sourceFiles, releaseMode, outputPath)
            }
            otherBins.forEach { otherBin ->
                val otherBinName = otherBin.nameWithoutExtension
                val otherOutputPath = "$baseOutPath/$otherBinName"
                compileBin(otherBin, sourceFiles, releaseMode, otherOutputPath)
            }
        }
        term.println(buildString {
            append(success("Finished"))
            if (releaseMode) {
                append(" release [optimized] target(s)")
            } else {
                append(" dev [unoptimized + debuginfo] target(s)")
            }
            append(" in ${compileDuration}s")
        })
    }
}

private fun compileBin(
    mainSource: File,
    sourceFiles: List<File>,
    releaseMode: Boolean,
    outputPath: String,
) {
    exec {
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
}
