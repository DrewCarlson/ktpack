package ktpack.compilation

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import ksubprocess.*
import ktfio.*
import ktpack.*
import ktpack.compilation.dependencies.MavenDependencyResolver
import ktpack.compilation.dependencies.models.DependencyNode
import ktpack.compilation.dependencies.models.printDependencyTree
import ktpack.compilation.dependencies.models.shakeAndFlattenDependencies
import ktpack.configuration.*
import ktpack.util.*
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random

// https://kotlinlang.org/docs/compiler-reference.html
class ModuleBuilder(
    private val module: ModuleConf,
    private val context: CliContext,
    val modulePath: Path,
) {
    data class CollectedSource(
        val sourceFiles: List<String>,
        val mainFile: String?,
        val binFiles: List<String>,
    ) {
        val hasLibFile = mainFile != null || binFiles.isNotEmpty()
        val isEmpty = mainFile == null && sourceFiles.isEmpty() && binFiles.isEmpty()
    }

    enum class BuildType { BIN, LIB }

    private val logger = Logger.withTag(ModuleBuilder::class.simpleName.orEmpty())
    private val outFolder = modulePath / "out"
    private val srcFolder = modulePath / "src"

    private val resolver = MavenDependencyResolver(module, context.http)

    private val targetFolderAliases = mapOf(
        KotlinTarget.JVM to listOf("jvm"),
        KotlinTarget.JS_NODE to listOf("js", "jsnode"),
        KotlinTarget.JS_BROWSER to listOf("js", "jsbrowser"),
        KotlinTarget.MACOS_ARM64 to listOf("native", "macos", "posix", "macosarm64"),
        KotlinTarget.MACOS_X64 to listOf("native", "macos", "posix", "macosx64"),
        KotlinTarget.MINGW_X64 to listOf("native", "mingw", "mingwx64"),
        KotlinTarget.LINUX_ARM64 to listOf("native", "linux", "posix", "linuxarm64"),
        KotlinTarget.LINUX_X64 to listOf("native", "linux", "posix", "linuxx64"),
    )

    init {
        require(modulePath.isAbsolute) {
            "ModuleBuilder requires modulePath to be absolute: $modulePath"
        }
    }

    fun collectSourceFiles(target: KotlinTarget, buildType: BuildType): CollectedSource {
        check(srcFolder.isDirectory()) { "Expected directory at $srcFolder" }
        val (mainFileName, secondaryDir) = when (buildType) {
            BuildType.BIN -> "main.kt" to "bin"
            BuildType.LIB -> "lib.kt" to "lib"
        }
        val sourceAliases = targetFolderAliases.getValue(target)
        val targetKotlinRoots = (sourceAliases + "common").mapNotNull { alias ->
            (srcFolder / alias / "kotlin").takeIf(Path::exists)
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

        return CollectedSource(
            sourceFiles = sourceFiles,
            mainFile = mainFile?.toString(),
            binFiles = binFiles,
        )
    }

    suspend fun buildBin(
        releaseMode: Boolean,
        binName: String,
        target: KotlinTarget,
        libs: List<String>? = null,
    ): ArtifactResult = Dispatchers.Default {
        val dependencyTree = resolveDependencyTree(module, modulePath, listOf(target))

        val collectedSourceFiles = collectSourceFiles(target, BuildType.BIN)
        if (collectedSourceFiles.isEmpty) {
            return@Default ArtifactResult.NoSourceFiles
        }
        val selectedBinFile = if (binName == module.name) {
            collectedSourceFiles.mainFile?.run(::File)
        } else {
            collectedSourceFiles.binFiles.firstNotNullOf { filePath ->
                File(filePath).takeIf { it.nameWithoutExtension == binName }
            }
        } ?: return@Default ArtifactResult.NoArtifactFound

        val modeString = if (releaseMode) "release" else "debug"
        val targetBinDir = outFolder / target.name.lowercase() / modeString / "bin"
        if (!targetBinDir.exists()) targetBinDir.mkdirs()
        val outputPath = targetBinDir / binName
        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, releaseMode, target)
            .flatMap { it.artifacts }
        val (result, duration) = measureSeconds {
            startKotlinCompiler(
                selectedBinFile,
                collectedSourceFiles.sourceFiles,
                releaseMode,
                outputPath,
                target,
                true,
                resolvedLibs,
            )
        }
        return@Default if (result.exitCode == 0) {
            ArtifactResult.Success(
                target = target,
                compilationDuration = duration,
                dependencyArtifacts = resolvedLibs,
                artifactPath = "${outputPath}${target.executableExt}",
                outputText = listOf(result.output, result.errors).joinToString("\n"),
            )
        } else {
            ArtifactResult.ProcessError(result.exitCode, result.errors.ifBlank { result.output })
        }
    }

    suspend fun buildLib(
        releaseMode: Boolean,
        target: KotlinTarget,
        libs: List<String>? = null,
    ): ArtifactResult = Dispatchers.Default {
        val collectedSourceFiles = collectSourceFiles(target, BuildType.LIB)
        val sourceFiles = collectedSourceFiles.sourceFiles
        if (collectedSourceFiles.isEmpty || !collectedSourceFiles.hasLibFile) {
            return@Default ArtifactResult.NoArtifactFound
        }
        val libFile = File(checkNotNull(collectedSourceFiles.mainFile))
        val dependencyTree = resolveDependencyTree(module, modulePath, listOf(target))

        val modeString = if (releaseMode) "release" else "debug"
        val targetLibDir = outFolder / target.name.lowercase() / modeString / "lib"
        if (!targetLibDir.exists()) targetLibDir.mkdirs()

        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, releaseMode, target)
            .flatMap { it.artifacts }
        val outputPath = targetLibDir / module.name
        val (result, duration) = measureSeconds {
            startKotlinCompiler(
                libFile,
                sourceFiles,
                releaseMode,
                outputPath,
                target,
                false,
                resolvedLibs,
            )
        }
        return@Default if (result.exitCode == 0) {
            ArtifactResult.Success(
                target = target,
                artifactPath = "${outputPath}${target.libraryExt}",
                compilationDuration = duration,
                outputText = listOf(result.output, result.errors).joinToString("\n"),
                dependencyArtifacts = resolvedLibs,
            )
        } else {
            ArtifactResult.ProcessError(result.exitCode, result.errors.ifBlank { result.output })
        }
    }

    suspend fun buildAllBins(releaseMode: Boolean, target: KotlinTarget): List<ArtifactResult> {
        val dependencyTree = resolveDependencyTree(module, modulePath, listOf(target))

        logger.d { dependencyTree.printDependencyTree() }

        if (!outFolder.exists() && !outFolder.mkdirs().exists()) {
            error("Could not create build folder: $outFolder")
        }

        val resolvedDeps = assembleDependencies(dependencyTree, releaseMode, target)
            .flatMap { it.artifacts }
        val sourceFiles = collectSourceFiles(target, BuildType.BIN) // TODO: Scan only for main.kt and bin files
        val mainSource: Path? = sourceFiles.mainFile?.toPath()
        return listOfNotNull(
            if (mainSource?.exists() == true) {
                buildBin(releaseMode, module.name, target, resolvedDeps)
            } else {
                null
            },
        ) + sourceFiles.binFiles.map { otherBin ->
            buildBin(releaseMode, otherBin.toPath().nameWithoutExtension, target, resolvedDeps)
        }
    }

    suspend fun buildAll(releaseMode: Boolean, target: KotlinTarget): List<ArtifactResult> {
        return buildAllBins(releaseMode, target) + buildLib(releaseMode, target)
    }

    private suspend fun assembleDependencies(
        root: List<DependencyNode>,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): List<DependencyNode> {
        val (newRoot, duration) = measureSeconds {
            val (mavenDependencies, mavenDuration) = measureSeconds {
                val deps = root.filter { it.dependencyConf is DependencyConf.MavenDependency }
                    .shakeAndFlattenDependencies()
                resolver.resolveArtifacts(deps, releaseMode, target)
            }

            logger.d { "Assembled maven dependencies in ${mavenDuration}s" }

            val (localDependencies, localDuration) = measureSeconds {
                root.filter { it.dependencyConf is DependencyConf.LocalPathDependency }
                    .map { child -> buildChildDependency(child, releaseMode, target, downloadArtifacts = true) }
            }

            logger.d { "Assembled local dependencies in ${localDuration}s" }
            localDependencies + mavenDependencies
        }

        logger.d { "Assembled dependencies in ${duration}s" }

        return newRoot
    }

    private suspend fun buildChildDependency(
        child: DependencyNode,
        releaseMode: Boolean,
        target: KotlinTarget,
        downloadArtifacts: Boolean,
        libs: List<String>? = null,
    ): DependencyNode {
        val localModule = checkNotNull(child.localModule)
        val childPath = modulePath / localModule.name
        val childBuilder = ModuleBuilder(localModule, context, childPath)

        val innerDepNodes = child.children
            .filter { it.dependencyConf is DependencyConf.LocalPathDependency }
            .map { innerChild ->
                childBuilder.buildChildDependency(innerChild, releaseMode, target, downloadArtifacts)
            }

        val innerLibs = innerDepNodes.flatMap { it.artifacts } + libs.orEmpty()
        val result = if (downloadArtifacts) {
            when (val result = childBuilder.buildLib(releaseMode, target, innerLibs)) {
                is ArtifactResult.Success -> result.artifactPath
                ArtifactResult.NoSourceFiles -> null
                ArtifactResult.NoArtifactFound -> error("No artifact found at $childPath")
                is ArtifactResult.ProcessError -> error(result.message.orEmpty())
            }
        } else {
            null
        }

        return child.copy(
            children = innerDepNodes,
            artifacts = listOfNotNull(result),
        )
    }

    suspend fun resolveDependencyTree(
        rootModule: ModuleConf,
        rootFolder: Path,
        targets: List<KotlinTarget>,
    ): List<DependencyNode> {
        return rootModule.dependencies
            .filter { it.targets.isEmpty() || (targets.isNotEmpty() && it.targets.containsAll(targets)) }
            .flatMap { it.dependencies }
            .map { dependency ->
                when (dependency) {
                    is DependencyConf.LocalPathDependency -> resolveLocalDependency(dependency, rootFolder, targets)
                    is DependencyConf.MavenDependency -> resolveMavenDependency(dependency, targets)
                    is DependencyConf.GitDependency -> TODO()
                    is DependencyConf.NpmDependency -> TODO()
                }
            }
    }

    private suspend fun resolveMavenDependency(
        dependencyConf: DependencyConf.MavenDependency,
        targets: List<KotlinTarget>,
    ): DependencyNode {
        val node = DependencyNode(
            localModule = null,
            dependencyConf = dependencyConf,
            children = emptyList(),
            artifacts = emptyList(),
        )
        // TODO: address default target selection
        return resolver.resolve(node, false, targets.firstOrNull() ?: KotlinTarget.JVM)
    }

    private suspend fun resolveLocalDependency(
        dependencyConf: DependencyConf.LocalPathDependency,
        rootFolder: Path,
        targets: List<KotlinTarget>,
    ): DependencyNode {
        val packFile = rootFolder / dependencyConf.path / PACK_SCRIPT_FILENAME
        val localModule = context.loadKtpackConf(packFile.toString()).module
        val children = resolveDependencyTree(localModule, rootFolder / dependencyConf.path, targets)
        return DependencyNode(
            localModule = localModule,
            dependencyConf = dependencyConf,
            children = children,
            artifacts = emptyList(),
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun startKotlinCompiler(
        mainSource: File?,
        sourceFiles: List<String>,
        releaseMode: Boolean,
        output: Path,
        target: KotlinTarget,
        isBinary: Boolean,
        libs: List<String>,
    ): CommunicateResult {
        check(output.isAbsolute) {
            "ModuleBuilder.startKotlinCompiler(...) requires `output` to be an absolute path: $output"
        }
        val outputDir = checkNotNull(output.parent).mkdirs()
        val compileOpts = outputDir / "${Random.Default.nextBytes(6).toHexString()}.opts"
        val kotlinVersion = module.kotlinVersion ?: Ktpack.KOTLIN_VERSION
        return try {
            exec {
                workingDirectory = outputDir.toString()
                when (target) {
                    KotlinTarget.JVM -> {
                        configureJvmCompilerArgs(output, target, kotlinVersion, libs, compileOpts)
                    }

                    KotlinTarget.JS_NODE, KotlinTarget.JS_BROWSER -> {
                        configureJsCompilerArgs(kotlinVersion, releaseMode, output, isBinary, libs, compileOpts)
                    }

                    KotlinTarget.MACOS_ARM64,
                    KotlinTarget.MACOS_X64,
                    KotlinTarget.MINGW_X64,
                    KotlinTarget.LINUX_ARM64,
                    KotlinTarget.LINUX_X64,
                    -> {
                        configureNativeCompilerArgs(
                            isBinary,
                            output,
                            target,
                            kotlinVersion,
                            releaseMode,
                            libs,
                            compileOpts,
                        )
                    }
                }

                configureCommonCompilerArgs(mainSource, sourceFiles, compileOpts)
            }
        } finally {
            compileOpts.delete()
        }
    }

    private fun ExecArgumentsBuilder.configureCommonCompilerArgs(
        mainSource: File?,
        sourceFiles: List<String>,
        compileOpts: Path,
    ) {
        arg("-verbose")
        // arg("-nowarn")
        // arg("-Werror")

        arg("-Xmulti-platform")

        //val serializationPlugin = File(context.kotlinInstalls.findKotlinHome(kotlinVersion), "lib")
        //    .nestedFile("kotlinx-serialization-compiler-plugin.jar")
        // arg("-Xplugin=${serializationPlugin.getAbsolutePath()}")

        // args("-kotlin-home", path)
        // args("-opt-in", <class>)

        mainSource?.getAbsolutePath()?.run(::arg)
        sourceFiles.forEach { file -> arg(file) }

        logger.d { "Launching Kotlin Compiler:\n${arguments.joinToString("\n")}" }
        if (compileOpts.exists()) {
            logger.d { "${compileOpts.name}:\n${compileOpts.readUtf8()}" }
        }
    }

    private fun ExecArgumentsBuilder.configureJvmCompilerArgs(
        outputPath: Path,
        target: KotlinTarget,
        kotlinVersion: String,
        libs: List<String>,
        compileOpts: Path,
    ) {
        val targetOutPath = "${outputPath}${target.executableExt}"
        arg(context.kotlinInstalls.findKotlincJvm(kotlinVersion))

        // arg("-Xjdk-release=$version")
        // args("-jvm-target", "1.8")
        arg("-include-runtime")
        // args("-jdk-home", "")

        if (libs.isNotEmpty()) {
            val opts = libs.joinToString(CPSEP, prefix = "-classpath ")
            compileOpts.writeUtf8(opts) { error ->
                throw error
            }
            arg("@${compileOpts.name}")
        }

        args("-d", targetOutPath) // output folder/ZIP/Jar path
    }

    private fun ExecArgumentsBuilder.configureJsCompilerArgs(
        kotlinVersion: String,
        releaseMode: Boolean,
        output: Path,
        isBinary: Boolean,
        libs: List<String>,
        compileOpts: Path,
    ) {
        val kotlincPath = context.kotlinInstalls.findKotlincJs(kotlinVersion).toPath()
        arg(kotlincPath.toString())

        args("-main", if (isBinary) "call" else "noCall")
        args("-module-kind", "umd") // umd|commonjs|amd|plain

        //arg("-Xir-only")
        if (releaseMode) {
            arg("-Xir-dce")
            arg("-Xoptimize-generated-js")
        }

        //arg("-Xir-build-cache")
        //arg("")
        //compileOpts.appendUtf8("-Xcache-directory=${output.parent}", onError = { throw it })

        //arg("-Xgenerate-dts")

        arg("-Xir-produce-js")
        args("-ir-output-dir", output.parent!!.toString())
        args("-ir-output-name", output.name)
        //args("-output", targetOutPath.toString()) // output js file

        if (!isBinary) {
            arg("-meta-info")
        }
        //arg("-source-map")
        // args("-source-map-base-dirs", <path>)
        // args("-source-map-embed-sources", "never") // always|never|inlining


        // arg("-no-stdlib")
        // Later version of kotlinc-js fail to provide the sdk by default
        val jsLibs = libs + (kotlincPath.parent?.parent!! / "lib" / "kotlin-stdlib-js.jar").toString()
        if (jsLibs.isNotEmpty()) {
            val archiveLibFiles = jsLibs.filter {
                it.endsWith(".klib") || it.endsWith(".jar")
            }

            val optString = archiveLibFiles.joinToString(CPSEP, prefix = "-libraries ")
            compileOpts.appendUtf8(optString, onError = { throw it })
        }
        arg("@${compileOpts.name}")
    }

    private fun ExecArgumentsBuilder.configureNativeCompilerArgs(
        isBinary: Boolean,
        outputPath: Path,
        target: KotlinTarget,
        kotlinVersion: String,
        releaseMode: Boolean,
        libs: List<String>,
        compileOpts: Path,
    ) {
        val targetOutPath = if (isBinary) {
            outputPath / target.executableExt
        } else {
            outputPath // library suffix will be added be the compiler
        }
        arg(context.kotlinInstalls.findKotlincNative(kotlinVersion))
        targetOutPath.parent?.mkdirs()

        args("-output", targetOutPath.toString()) // output kexe or exe file
        // args("-entry", "") // TODO: Handle non-root package main funcs

        if (isBinary) {
            args("-produce", "program")
        } else {
            args("-produce", "library") // static, dynamic, framework, library, bitcode
        }

        if (releaseMode) {
            arg("-opt") // kotlinc-native only: enable compilation optimizations
        } else {
            arg("-g") // kotlinc-native only: emit debug info
        }

        if (libs.isNotEmpty()) {
            val libFlag = "-library "
            val libList = libs.joinToString("\n") {
                libFlag + it
            }
            compileOpts.writeUtf8(libList) { throw it }
            args("@${compileOpts.name}")
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

        args("-target", target.name.lowercase())
    }
}
