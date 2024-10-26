package ktpack.compilation

import co.touchlab.kermit.Logger
import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.invoke
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import ksubprocess.*
import ktpack.*
import ktpack.compilation.dependencies.MavenDependencyResolver
import ktpack.compilation.dependencies.models.DependencyNode
import ktpack.compilation.dependencies.models.resolveAndFlatten
import ktpack.configuration.*
import ktpack.manifest.DependencyToml
import ktpack.manifest.ManifestToml
import ktpack.manifest.OutputToml
import ktpack.util.*

/**
 * This class uses a Ktpack [ModuleConf] generated by a pack.toml manifest
 * and a specific action selected by the CLI to produce kotlin compiler
 * arguments with the correct list of kotlin source files and libraries.
 *
 * @param module The root module configuration to build.
 * @param context The cli run context.
 * @param modulePath The root directory containing the pack.toml manifest.
 */
// https://github.com/JetBrains/kotlin/blob/master/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/K2NativeCompilerArguments.kt
// https://github.com/JetBrains/kotlin/blob/master/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/K2JSCompilerArguments.kt
// https://github.com/JetBrains/kotlin/blob/master/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/K2JVMCompilerArguments.kt
// https://github.com/JetBrains/kotlin/blob/master/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/CommonCompilerArguments.kt
// https://kotlinlang.org/docs/compiler-reference.html
class ModuleBuilder(
    private val manifest: ManifestToml,
    private val context: CliContext,
    val modulePath: Path,
) {

    enum class BuildType { BIN, LIB, TEST }

    private val logger = Logger.withTag(ModuleBuilder::class.simpleName.orEmpty())
    private val outFolder = Path(modulePath, "out")
    private val srcFolder = Path(modulePath, "src")

    private val module = manifest.module
    private val resolver = MavenDependencyResolver(context.http)
    internal val sourceCollector = KtpackSourceCollector(srcFolder)

    init {
        require(modulePath.isAbsolute) {
            "ModuleBuilder requires modulePath to be absolute: $modulePath"
        }
    }

    suspend fun buildCommonLib(
        platformSources: CollectedSource,
        releaseMode: Boolean,
        target: KotlinTarget,
        buildType: BuildType,
        libs: List<String>? = null,
    ): ArtifactResult = Dispatchers.IO {
        val dependencyScopes = listOf(DependencyScope.IMPLEMENTATION, DependencyScope.API)
        val dependencyTree = resolveRootDependencyTree(listOf(target), dependencyScopes, includeCommon = true)

        val collectedSourceFiles = sourceCollector.collectKotlin(null, buildType)
        if (collectedSourceFiles.isEmpty && platformSources.isEmpty) {
            return@IO ArtifactResult.NoSourceFiles
        }

        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, false, target)
            .flatMap { it.artifacts }
        val modeString = if (releaseMode) "release" else "debug"
        val commonLibOutput = Path(outFolder, target.name.lowercase(), modeString, "klib", "common")
        commonLibOutput.parent?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
        val (result, duration) = measureSeconds {
            startKotlinCompiler(
                sourceFiles = collectedSourceFiles.merge(platformSources).sourceFiles,
                releaseMode = releaseMode,
                output = commonLibOutput,
                target = target,
                isBinary = false,
                libs = resolvedLibs,
                isTest = false,
                isMultiplatform = true,
            )
        }

        if (result.exitCode == 0) {
            ArtifactResult.Success(
                target = target,
                artifactPath = "${commonLibOutput}.klib",
                compilationDuration = duration,
                outputText = listOf(result.output, result.errors).joinToString("\n"),
                dependencyArtifacts = resolvedLibs,
            )
        } else {
            ArtifactResult.ProcessError(result.exitCode, result.output)
        }
    }

    suspend fun buildTest(
        target: KotlinTarget,
        libs: List<String>? = null,
    ): ArtifactResult = Dispatchers.IO {
        val dependencyTree = resolveRootDependencyTree(listOf(target), DependencyScope.entries)

        val collectedSourceFiles = sourceCollector.collectKotlin(target, BuildType.TEST)
        if (collectedSourceFiles.isEmpty) {
            return@IO ArtifactResult.NoSourceFiles
        }

        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, false, target)
            .flatMap { it.artifacts }
        val (result, duration) = measureSeconds {
            startKotlinCompiler(
                sourceFiles = collectedSourceFiles.sourceFiles,
                releaseMode = false,
                output = Path(outFolder, "test", target.name.lowercase(), module.name),
                target = target,
                isBinary = true,
                libs = resolvedLibs,
                isTest = true,
            )
        }

        val outputPath = Path(outFolder, "test", target.name.lowercase(), "${module.name}${target.executableExt}")

        return@IO if (result.exitCode == 0) {
            ArtifactResult.Success(
                target = target,
                compilationDuration = duration,
                dependencyArtifacts = resolvedLibs,
                artifactPath = outputPath.toString(),
                outputText = listOf(result.output, result.errors).joinToString("\n"),
            )
        } else {
            ArtifactResult.ProcessError(result.exitCode, result.errors.ifBlank { result.output })
        }
    }

    suspend fun buildBin(
        releaseMode: Boolean,
        binName: String,
        target: KotlinTarget,
        libs: List<String>? = null,
    ): ArtifactResult = Dispatchers.IO {
        val collectedSourceFiles = sourceCollector.collectKotlin(target, BuildType.BIN)
        if (collectedSourceFiles.isEmpty) {
            // TODO: Restore this check, but consider common sources
            //return@IO ArtifactResult.NoSourceFiles
        }

        val commonLibPath = if (target == KotlinTarget.JVM) {
            null
        } else {
            when (val result = buildCommonLib(collectedSourceFiles, releaseMode, target, BuildType.BIN, libs)) {
                is ArtifactResult.Success -> result.artifactPath
                ArtifactResult.NoSourceFiles -> null
                ArtifactResult.NoArtifactFound,
                is ArtifactResult.ProcessError,
                    -> return@IO result
            }
        }

        val dependencyTree = resolveRootDependencyTree(listOf(target), includeCommon = true)

        val modeString = if (releaseMode) "release" else "debug"
        val targetBinDir = Path(outFolder, target.name.lowercase(), modeString, "bin")
        if (!targetBinDir.exists()) targetBinDir.mkdirs()
        val outputPath = Path(targetBinDir, binName)
        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, releaseMode, target)
            .flatMap { it.artifacts }
        // TODO: Improve handling for JVM (one-shot) MPP compilation
        val (result, duration) = measureSeconds {
            startKotlinCompiler(
                sourceFiles = if (commonLibPath == null) {
                    collectedSourceFiles.sourceFiles
                } else {
                    emptyList()
                },
                releaseMode = releaseMode,
                output = outputPath,
                target = target,
                isBinary = true,
                isTest = false,
                isMultiplatform = commonLibPath == null,
                includes = listOfNotNull(commonLibPath),
                libs = resolvedLibs,
            )
        }
        return@IO if (result.exitCode == 0) {
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
        val collectedSourceFiles = sourceCollector.collectKotlin(target, BuildType.LIB)

        val commonLibPath =
            (buildCommonLib(collectedSourceFiles, releaseMode, target, BuildType.LIB, libs) as ArtifactResult.Success)
                .artifactPath

        val sourceFiles = collectedSourceFiles.sourceFiles
        if (collectedSourceFiles.isEmpty) {
            return@Default ArtifactResult.NoArtifactFound
        }
        val dependencyTree = resolveRootDependencyTree(listOf(target))

        val modeString = if (releaseMode) "release" else "debug"
        val targetLibDir = Path(outFolder, target.name.lowercase(), modeString, "lib")
        if (!targetLibDir.exists()) targetLibDir.mkdirs()

        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, releaseMode, target)
            .flatMap { it.artifacts }
        val outputPath = Path(targetLibDir, module.name)
        val (result, duration) = measureSeconds {
            startKotlinCompiler(
                sourceFiles = sourceFiles,
                releaseMode = releaseMode,
                output = outputPath,
                target = target,
                isBinary = false,
                isTest = false,
                libs = listOfNotNull(commonLibPath) + resolvedLibs,
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

    suspend fun build(releaseMode: Boolean, target: KotlinTarget): ArtifactResult {
        val mainOutput = module.output ?: sourceCollector.getDefaultOutput()
        return buildOutput(mainOutput, releaseMode)
    }

    suspend fun buildOutput(output: OutputToml, releaseMode: Boolean): ArtifactResult {
        val hostTarget = PlatformUtils.getHostTarget()
        val outputName = output.name ?: module.name
        return when (output) {
            is OutputToml.Lib -> buildLib(releaseMode, hostTarget)
            is OutputToml.BinCommon.JvmBin -> buildBin(releaseMode, outputName, KotlinTarget.JVM)
            // TODO: Validate target support
            is OutputToml.BinCommon.Bin,
            is OutputToml.BinCommon.LinuxBin,
            is OutputToml.BinCommon.MacosBin,
            is OutputToml.BinCommon.WindowsBin,
                -> buildBin(releaseMode, outputName, hostTarget)
        }
    }

    private suspend fun assembleDependencies(
        root: List<DependencyNode>,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): List<DependencyNode> {
        val (newRoot, duration) = measureSeconds {
            val (mavenDependencies, mavenDuration) = measureSeconds {
                val deps = root.filter { it.dependencyConf is DependencyToml.Maven }
                    .resolveAndFlatten()
                resolver.resolveArtifacts(deps, releaseMode, target)
            }

            logger.d { "Assembled maven dependencies in ${mavenDuration}s" }

            val (localDependencies, localDuration) = measureSeconds {
                root.filter { it.dependencyConf is DependencyToml.Local }
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
        val localManifest = checkNotNull(child.localManifest)
        val childPath = Path(modulePath, localManifest.module.name)
        val childBuilder = ModuleBuilder(localManifest, context, childPath)

        val innerDepNodes = child.children
            .filter { it.dependencyConf is DependencyToml.Local }
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

    suspend fun resolveRootDependencyTree(
        targets: List<KotlinTarget>,
        scopes: List<DependencyScope> = listOf(DependencyScope.IMPLEMENTATION),
        includeCommon: Boolean = true,
    ): List<DependencyNode> {
        return resolveDependencyTree(manifest, workingDirectory, targets, scopes, includeCommon)
    }

    private suspend fun resolveDependencyTree(
        rootManifest: ManifestToml,
        rootFolder: Path,
        targets: List<KotlinTarget>,
        scopes: List<DependencyScope>,
        includeCommon: Boolean,
    ): List<DependencyNode> {
        return rootManifest.dependenciesFor(scopes, targets, includeCommon = includeCommon)
            .flatMap { it.value.values }
            .distinctBy { it.key }
            .map { dependency ->
                when (dependency) {
                    is DependencyToml.Local -> resolveLocalDependency(
                        dependencyConf = dependency,
                        rootFolder = rootFolder,
                        targets = targets,
                        scopes = scopes,
                        includeCommon = includeCommon,
                    )

                    is DependencyToml.Maven -> resolveMavenDependency(dependency, targets)
                    is DependencyToml.Git -> TODO()
                    is DependencyToml.Npm -> TODO()
                }
            }
    }

    suspend fun fetchArtifacts(
        root: List<DependencyNode>,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): List<DependencyNode> {
        // TODO: Don't build child dependencies, just download remote artifacts/sources
        return assembleDependencies(root, releaseMode, target)
    }

    private suspend fun resolveMavenDependency(
        dependencyConf: DependencyToml.Maven,
        targets: List<KotlinTarget>,
    ): DependencyNode {
        val node = DependencyNode(
            localManifest = null,
            dependencyConf = dependencyConf,
            children = emptyList(),
            artifacts = emptyList(),
        )
        // TODO: address default target selection
        return resolver.resolve(node, false, targets.first())
    }

    private suspend fun resolveLocalDependency(
        dependencyConf: DependencyToml.Local,
        rootFolder: Path,
        targets: List<KotlinTarget>,
        scopes: List<DependencyScope>,
        includeCommon: Boolean,
    ): DependencyNode {
        val packFile = Path(rootFolder, dependencyConf.path, MANIFEST_FILENAME)
        val localModule = context.loadManifestToml(packFile.toString())
        val children = resolveDependencyTree(
            localModule,
            Path(rootFolder, dependencyConf.path),
            targets,
            scopes,
            includeCommon = includeCommon,
        )
        return DependencyNode(
            localManifest = localModule,
            dependencyConf = dependencyConf,
            children = children,
            artifacts = emptyList(),
        )
    }

    private suspend fun startKotlinCompiler(
        sourceFiles: List<String>,
        releaseMode: Boolean,
        output: Path,
        target: KotlinTarget,
        isBinary: Boolean,
        isTest: Boolean,
        libs: List<String>,
        includes: List<String> = emptyList(),
        isMultiplatform: Boolean = false,
    ): CommunicateResult {
        check(output.isAbsolute) {
            "ModuleBuilder.startKotlinCompiler(...) requires `output` to be an absolute path: $output"
        }
        val defaultJdk = checkNotNull(context.jdkInstalls.getDefaultJdk()) {
            "Run `ktpack setup` before using your project"
        }
        val outputDir = checkNotNull(output.parent).mkdirs()
        val kotlinVersion = module.kotlinVersion ?: Ktpack.KOTLIN_VERSION
        val argsList = buildList<String> {
            if (target.isJs) {
                configureJsCompilerArgs(
                    kotlinVersion,
                    releaseMode,
                    output,
                    isBinary,
                    libs,
                    includes,
                )
            } else if (target.isNative) {
                configureNativeCompilerArgs(
                    isBinary = isBinary,
                    outputPath = output,
                    target = target,
                    releaseMode = releaseMode,
                    libs = libs,
                    isTest = isTest,
                    includes = includes,
                )
            } else {
                configureJvmCompilerArgs(output, libs)
            }

            configureCommonCompilerArgs(isMultiplatform)

            addAll(sourceFiles)
        }
        val kotlincPath = if (target.isJs) {
            context.kotlinInstalls.findKotlincJs(kotlinVersion)
        } else if (target.isNative) {
            context.kotlinInstalls.findKotlincNative(kotlinVersion)
        } else {
            context.kotlinInstalls.findKotlincJvm(kotlinVersion)
        }
        logger.d { "Launching Kotlin Compiler: $kotlincPath ${argsList.joinToString(" ")}" }
        return withKotlinCompilerArgFile(argsList) { argsFile ->
            logger.d { "Launching Kotlin Compiler: $kotlincPath @$argsFile" }
            exec {
                workingDirectory = outputDir.toString()
                environment["JAVA_HOME"] = defaultJdk.path
                environment["TMP"] = SystemTemporaryDirectory.toString()
                environment["JAVA_OPTS"] = "-Xmx1g"
                if (KotlinTarget.ALL_WINDOWS.contains(PlatformUtils.getHostTarget())) {
                    environment["SystemRoot"] = getEnv("windir").orEmpty()
                }

                arg(kotlincPath)

                arg("@$argsFile")
            }
        }
    }


    private inline fun <R> withKotlinCompilerArgFile(
        args: List<String>,
        block: (Path) -> R,
    ): R {
        // escaping rules from https://github.com/JetBrains/kotlin/blob/6161f44d91e235750077e1aaa5faff7047316190/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/preprocessCommandLineArguments.kt#L83
        val argString = args.joinToString(" ") { arg ->
            if (arg.contains(" ") || arg.contains("'")) {
                "'${arg.replace("\\", "\\\\").replace("'", "\\'")}'"
            } else {
                arg
            }
        }

        val argFile = Path(SystemTemporaryDirectory, "kotlin-args.txt")
        return try {
            argFile.writeString(argString)
            block(argFile)
        } finally {
            argFile.delete()
        }
    }

    private fun MutableList<String>.configureCommonCompilerArgs(isMultiplatform: Boolean) {
        if (context.debug) {
            add("-verbose")
        }
        // arg("-nowarn")
        // arg("-Werror")

        if (isMultiplatform) {
            add("-Xmulti-platform")
        }
        //val serializationPlugin = File(context.kotlinInstalls.findKotlinHome(kotlinVersion), "lib")
        //    .nestedFile("kotlinx-serialization-compiler-plugin.jar")
        // arg("-Xplugin=${serializationPlugin.getAbsolutePath()}")

        // args("-kotlin-home", path)
        // args("-opt-in", <class>)

        // args("-language-version", "2.0")
    }

    private fun MutableList<String>.configureJvmCompilerArgs(
        outputPath: Path,
        libs: List<String>,
    ) {
        val targetOutPath = "${outputPath}${KotlinTarget.JVM.executableExt}"

        // arg("-Xjdk-release=$version")
        // args("-jvm-target", "1.8")
        add("-include-runtime")
        // args("-jdk-home", "")

        if (libs.isNotEmpty()) {
            add("-classpath")
            add(libs.joinToString(CPSEP))
        }

        // output folder/ZIP/Jar path
        add("-d")
        add(targetOutPath)
    }

    private fun MutableList<String>.configureJsCompilerArgs(
        kotlinVersion: String,
        releaseMode: Boolean,
        output: Path,
        isBinary: Boolean,
        libs: List<String>,
        includes: List<String>,
    ) {
        val kotlincPath = Path(context.kotlinInstalls.findKotlincJs(kotlinVersion))

        if (isBinary) {
            add("-main")
            add("call") // "noCall"

            // umd|commonjs|amd|plain
            add("-module-kind")
            add("umd")
        }

        //arg("-Xir-only")
        if (releaseMode) {
            add("-Xir-dce")
            add("-Xoptimize-generated-js")
        }

        //arg("-Xir-build-cache")
        //compileOpts.appendUtf8("-Xcache-directory=${output.parent}", onError = { throw it })

        //arg("-Xgenerate-dts")

        if (isBinary) {
            add("-Xir-produce-js")
        } else {
            add("-Xir-produce-klib-file")
        }

        add("-ir-output-dir")
        add(output.parent!!.toString())

        add("-ir-output-name")
        add(output.name)
        //args("-output", targetOutPath.toString()) // output js file

        add("-Xdisable-default-scripting-plugin")
        add("-Xir-property-lazy-initialization")

        if (!isBinary) {
            add("-meta-info")
        }
        //arg("-source-map")
        // args("-source-map-base-dirs", <path>)
        // args("-source-map-embed-sources", "never") // always|never|inlining

        if (includes.isNotEmpty()) {
            add(includes.joinToString(CPSEP, prefix = "-Xinclude="))
        }

        // arg("-no-stdlib")
        // Later version of kotlinc-js fail to provide the sdk by default
        val stdlibExtension = if (Version.parse(kotlinVersion).major >= 2) "klib" else "jar"
        val jsLibs = libs + Path(kotlincPath.parent?.parent!!, "lib", "kotlin-stdlib-js.$stdlibExtension").toString()
        if (jsLibs.isNotEmpty()) {
            val archiveLibFiles = jsLibs.filter {
                it.endsWith(".klib") || it.endsWith(".jar")
            }.map { SystemFileSystem.resolve(Path(it)) }

            add("-libraries")
            add(archiveLibFiles.joinToString(CPSEP))
        }
    }

    private fun MutableList<String>.configureNativeCompilerArgs(
        isBinary: Boolean,
        outputPath: Path,
        target: KotlinTarget,
        releaseMode: Boolean,
        libs: List<String>,
        includes: List<String>,
        isTest: Boolean,
    ) {
        val targetOutPath = if (isBinary) {
            Path("$outputPath${target.executableExt}")
        } else {
            outputPath // library suffix will be added be the compiler
        }
        targetOutPath.parent?.mkdirs()

        add("-target")
        add(target.name.lowercase())

        // output kexe or exe file
        add("-output")
        add(targetOutPath.toString())

        if (isTest) {
            add("-generate-test-runner")
            // args("-generate-worker-test-runner", "")
            // args("-generate-no-exit-test-runner", "")
        } else {
            // args("-entry", "") // TODO: Handle non-root package main funcs
        }

        if (isBinary || isTest) {
            add("-produce")
            add("program")
        } else {
            // static, dynamic, framework, library, bitcode
            add("-produce")
            add("library")
        }

        if (releaseMode) {
            add("-opt") // kotlinc-native only: enable compilation optimizations
        } else {
            add("-g") // kotlinc-native only: emit debug info
            add("-enable-assertions")
        }

        if (libs.isNotEmpty()) {
            libs.forEach { lib ->
                add("-library")
                add(lib)
            }
        }

        if (includes.isNotEmpty()) {
            add(includes.joinToString(";", prefix = "-Xinclude="))
        }

        // args("-manifest", "")
        // args("-module-name", "")
        // args("-nomain", "")
        // args("-nopack", "")
        // args("-no-default-libs", "") // TODO: Disable for COMMON_ONLY modules
        // args("-nostd", "")

        // args("-repo", "")
        // args("-linker-option", "")
        // args("-linker-options", "")
        // args("-native-library", "")
        // args("-include-binary", "")
    }
}
