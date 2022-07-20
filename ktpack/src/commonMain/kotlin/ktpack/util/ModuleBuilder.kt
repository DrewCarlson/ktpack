package ktpack.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import ksubprocess.*
import ktfio.*
import ktpack.Ktpack
import ktpack.commands.ktversions.KotlincInstalls
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

// https://kotlinlang.org/docs/compiler-reference.html
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

    suspend fun buildBin(
        releaseMode: Boolean,
        binName: String,
        target: Target
    ): ArtifactResult = Dispatchers.Default {
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        val mainSource = mainSource

        val outDir = File("out")
        if (!outDir.exists()) outDir.mkdirs()

        val selectedBinFile = if (binName == module.name) {
            checkNotNull(mainSource)
        } else {
            otherBins.find { it.nameWithoutExtension == binName }
        } ?: return@Default ArtifactResult.NoArtifactFound
        val outputPath = listOf(outDir.getAbsolutePath(), target.name.lowercase(), module.name)
            .joinToString(filePathSeparator.toString())
        val (result, duration) = measureSeconds {
            startKotlinCompiler(selectedBinFile, sourceFiles, releaseMode, outputPath, target, true)
        }
        return@Default if (result.exitCode == 0) {
            ArtifactResult.Success(
                artifactPath = "$outputPath.${getExeExtension(target)}",
                compilationDuration = duration,
                outputText = listOf(result.output, result.errors).joinToString("\n"),
            )
        } else {
            ArtifactResult.ProcessError(result.exitCode, result.errors.ifBlank { result.output })
        }
    }

    suspend fun buildLib(
        releaseMode: Boolean,
        target: Target
    ): ArtifactResult = Dispatchers.Default {
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        val outDir = File("out")
        if (!outDir.exists()) outDir.mkdirs()

        val libNames = listOf(module.name, "lib")
        val selectedBinFile = sourceFiles.firstOrNull { libNames.contains(it.nameWithoutExtension) }
            ?: return@Default ArtifactResult.NoArtifactFound
        val outputPath = listOf(outDir.getAbsolutePath(), target.name.lowercase(), module.name)
            .joinToString(filePathSeparator.toString())
        val (result, duration) = measureSeconds {
            startKotlinCompiler(selectedBinFile, sourceFiles, releaseMode, outputPath, target, false)
        }
        return@Default if (result.exitCode == 0) {
            ArtifactResult.Success(
                artifactPath = "$outputPath.${getExeExtension(target)}",
                compilationDuration = duration,
                outputText = listOf(result.output, result.errors).joinToString("\n"),
            )
        } else {
            ArtifactResult.ProcessError(result.exitCode, result.errors.ifBlank { result.output })
        }
    }

    suspend fun buildAllBins(releaseMode: Boolean, target: Target): List<ArtifactResult> {
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        val mainSource = mainSource

        val artifactPaths = mutableListOf<ArtifactResult>()

        val outDir = File("out")
        if (!outDir.exists()) outDir.mkdirs()

        if (mainSource?.exists() == true) {
            artifactPaths.add(buildBin(releaseMode, target.name.lowercase(), target))
        }
        otherBins.forEach { otherBin ->
            artifactPaths.add(buildBin(releaseMode, otherBin.nameWithoutExtension, target))
        }

        return artifactPaths.toList()
    }

    suspend fun buildAll(releaseMode: Boolean, target: Target): List<ArtifactResult> {
        return buildAllBins(releaseMode, target) + buildLib(releaseMode, target)
    }

    private fun getExeExtension(target: Target): String {
        return when (target) {
            Target.COMMON_ONLY -> throw IllegalArgumentException("COMMON_ONLY is not a supported executable target.")
            Target.JVM -> "jar"
            Target.WINDOWS_X64 -> "exe"
            Target.JS_NODE,
            Target.JS_BROWSER -> "js"
            Target.MACOS_ARM64,
            Target.MACOS_X64,
            Target.LINUX_X64 -> "kexe"
        }
    }

    private suspend fun startKotlinCompiler(
        mainSource: File,
        sourceFiles: List<File>,
        releaseMode: Boolean,
        outputPath: String,
        target: Target,
        isBinary: Boolean,
    ): CommunicateResult = exec {
        val kotlinVersion = module.kotlinVersion ?: Ktpack.KOTLIN_VERSION

        when (target) {
            Target.JVM -> {
                val targetOutPath = "${outputPath}.${getExeExtension(target)}"
                arg(KotlincInstalls.findKotlincJvm(kotlinVersion))

                args("-d", targetOutPath) //  output folder/ZIP/Jar path

                // arg("-Xjdk-release=$version")
                // args("-jvm-target", "1.8")
                // arg("-include-runtime")
                // args("-jdk-home", "")
                // args("-classpath", "") // entries separated by ; on Windows and : on macOS/Linux
            }
            Target.JS_NODE, Target.JS_BROWSER -> {
                val targetOutPath = "${outputPath}.${getExeExtension(target)}"
                arg(KotlincInstalls.findKotlincJs(kotlinVersion))

                args("-output", targetOutPath) // output js file

                args("-main", "call") // or noCall

                if (!isBinary) {
                    arg("-meta-info")
                }
                // arg("-source-map")
                // args("-source-map-base-dirs", <path>)
                // args("-source-map-embed-sources", "never") // always|never|inlining
                // arg("-no-stdlib")

                args("-module-kind", "umd") // umd|commonjs|amd|plain
            }
            Target.COMMON_ONLY -> throw IllegalArgumentException("COMMON_ONLY is not a supported compile target.")
            Target.MACOS_ARM64,
            Target.MACOS_X64,
            Target.WINDOWS_X64,
            Target.LINUX_X64 -> {
                val targetOutPath = if (isBinary) {
                    "${outputPath}.${getExeExtension(target)}"
                } else {
                    outputPath // library suffix will be added be the compiler
                }
                arg(KotlincInstalls.findKotlincNative(kotlinVersion))
                File(targetOutPath.substringBeforeLast(filePathSeparator)).mkdirs()

                args("-output", targetOutPath) //output kexe or exe file
                //args("-entry", "") // TODO: Handle non-root package main funcs

                if (isBinary) {
                    args("-produce", "program") // program, static, dynamic, framework, library, bitcode
                } else {
                    args("-produce", "library")
                }

                if (releaseMode) {
                    arg("-opt") // kotlinc-native only: enable compilation optimizations
                } else {
                    arg("-g") // kotlinc-native only: emit debug info
                }

                // args("-manifest", "")
                // args("-module-name", "")
                // args("-nomain", "")
                // args("-nopack", "")
                // args("-no-default-libs", "") // TODO: Disable for COMMON_ONLY modules
                // args("-nostd", "")

                // args("-repo", "")
                // args("-library", "")
                // args("-linker-option", "")
                // args("-linker-options", "")
                // args("-native-library", "")
                // args("-include-binary", "")

                // args("-generate-test-runner", "")
                // args("-generate-worker-test-runner", "")
                // args("-generate-no-exit-test-runner", "")

                val konanTarget = when (target) {
                    Target.WINDOWS_X64 -> "mingw_x64"
                    else -> target.name.lowercase()
                }
                args("-target", konanTarget)
            }
        }

        arg(mainSource.getAbsolutePath())
        sourceFiles.forEach { file ->
            arg(file.getAbsolutePath())
        }
        arg("-verbose")
        // arg("-nowarn")
        // arg("-Werror")

        arg("-Xmulti-platform")

        // args("-kotlin-home", path)
        // args("-opt-in", <class>)
        println(arguments.joinToString(" "))
    }
}
