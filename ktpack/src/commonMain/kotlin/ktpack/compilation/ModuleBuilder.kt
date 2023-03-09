package ktpack.compilation

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.serialization.*
import ksubprocess.*
import ktfio.*
import ktpack.*
import ktpack.compilation.dependencies.ChildDependencyNode
import ktpack.compilation.dependencies.RootDependencyNode
import ktpack.configuration.*
import ktpack.gradle.GradleModule
import ktpack.maven.*
import ktpack.util.CPSEP
import ktpack.util.KTPACK_ROOT
import ktpack.util.failed
import ktpack.util.measureSeconds
import kotlin.system.exitProcess

sealed class ArtifactResult {
    data class Success(
        val artifactPath: String,
        val compilationDuration: Double,
        val outputText: String,
        val target: KotlinTarget,
        val dependencyArtifacts: List<String>,
    ) : ArtifactResult()

    data class ProcessError(
        val exitCode: Int,
        val message: String?,
    ) : ArtifactResult()

    object NoArtifactFound : ArtifactResult()
    object NoSourceFiles : ArtifactResult()
}

// https://kotlinlang.org/docs/compiler-reference.html
class ModuleBuilder(
    private val module: ModuleConf,
    private val context: CliContext,
    private val basePath: String,
) {

    val mavenRepoUrl = "https://repo1.maven.org/maven2"

    private val mavenDepCache = mutableMapOf<String, ChildDependencyNode>()

    private val cacheRoot = File(KTPACK_ROOT, "maven-cache")
    private val moduleFolder = File(basePath)
    val outFolder = moduleFolder.nestedFile("out")
    val srcFolder = moduleFolder.nestedFile("src")

    private val targetFolderAliases = mutableMapOf(
        KotlinTarget.JVM to listOf("jvm"),
        KotlinTarget.JS_NODE to listOf("js", "jsnode"),
        KotlinTarget.JS_BROWSER to listOf("js", "jsbrowser"),
        KotlinTarget.MACOS_ARM64 to listOf("native", "macos", "posix", "macosarm64"),
        KotlinTarget.MACOS_X64 to listOf("native", "macos", "posix", "macosx64"),
        KotlinTarget.MINGW_X64 to listOf("native", "mingw", "mingwx64"),
        KotlinTarget.LINUX_ARM64 to listOf("native", "linux", "posix", "linuxarm64"),
        KotlinTarget.LINUX_X64 to listOf("native", "linux", "posix", "linuxx64"),
    )

    data class CollectedSource(
        val sourceFiles: List<String>,
        val mainFile: String?,
        val binFiles: List<String>,
    ) {
        val hasLibFile = mainFile != null || binFiles.isNotEmpty()
        val isEmpty = mainFile == null && sourceFiles.isEmpty() && binFiles.isEmpty()
    }

    enum class BuildType { BIN, LIB }

    fun collectSourceFiles(target: KotlinTarget, buildType: BuildType): CollectedSource {
        check(srcFolder.isDirectory()) { "Expected directory at ${srcFolder.getAbsolutePath()}" }
        val (mainFileName, secondaryDir) = when (buildType) {
            BuildType.BIN -> "main.kt" to "bin"
            BuildType.LIB -> "lib.kt" to "lib"
        }
        val sourceAliases = targetFolderAliases.getValue(target)
        val sourceFolderPath = srcFolder.getAbsolutePath()
        val targetKotlinRoots = (sourceAliases + "common").mapNotNull { alias ->
            File(sourceFolderPath, alias, "kotlin").takeIf(File::exists)
        }
        val sourceFiles = targetKotlinRoots.flatMap { targetKotlinRoot ->
            targetKotlinRoot.walkTopDown()
                .onEnter { folder ->
                    if (folder.getParentFileUnsafe() == targetKotlinRoot) {
                        folder.getName() != secondaryDir
                    } else true
                }
                .filter { file ->
                    val fileName = file.getName()
                    fileName.endsWith(".kt") && fileName != mainFileName
                }
                .map(File::getAbsolutePath)
                .toList()
        }
        val mainFile = targetKotlinRoots.firstNotNullOfOrNull { kotlinRoot ->
            kotlinRoot.nestedFile(mainFileName).takeIf(File::exists)
        }
        val binFiles = targetKotlinRoots.flatMap { kotlinRoot ->
            val binRoot = kotlinRoot.nestedFile(secondaryDir)
            if (binRoot.exists()) {
                binRoot.walk()
                    .drop(1) // Ignore the parent folder
                    .map(File::getAbsolutePath)
                    .toList()
            } else emptyList()
        }

        return CollectedSource(
            sourceFiles = sourceFiles,
            mainFile = mainFile?.getAbsolutePath(),
            binFiles = binFiles,
        )
    }

    suspend fun buildBin(
        releaseMode: Boolean,
        binName: String,
        target: KotlinTarget,
        libs: List<String>? = null,
    ): ArtifactResult = Dispatchers.Default {
        val dependencyTree = resolveDependencyTree(module, moduleFolder, listOf(target))

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
        val targetBinDir = File(outFolder.getAbsolutePath(), target.name.lowercase(), modeString, "bin")
        if (!targetBinDir.exists()) targetBinDir.mkdirs()
        val outputPath = targetBinDir.nestedFile(binName).getAbsolutePath()
        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, releaseMode, target, false)
            .resolve()
            .map { fetchMavenDependency(it, releaseMode, target, true) }
            .flatMap { it.artifacts }
        val (result, duration) = measureSeconds {
            startKotlinCompiler(
                selectedBinFile,
                collectedSourceFiles.sourceFiles,
                releaseMode,
                outputPath,
                target,
                true,
                resolvedLibs
            )
        }
        return@Default if (result.exitCode == 0) {
            ArtifactResult.Success(
                target = target,
                compilationDuration = duration,
                dependencyArtifacts = resolvedLibs,
                artifactPath = "${outputPath}${getExeExtension(target)}",
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
        val dependencyTree = resolveDependencyTree(module, moduleFolder, listOf(target))

        val modeString = if (releaseMode) "release" else "debug"
        val targetLibDir = File(outFolder.getAbsolutePath(), target.name.lowercase(), modeString, "lib")
        if (!targetLibDir.exists()) targetLibDir.mkdirs()

        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, releaseMode, target, true).artifacts
        val outputPath = listOf(targetLibDir.getAbsolutePath(), module.name)
            .joinToString(filePathSeparator.toString())
        val (result, duration) = measureSeconds {
            startKotlinCompiler(
                libFile,
                sourceFiles,
                releaseMode,
                outputPath,
                target,
                false,
                resolvedLibs
            )
        }
        return@Default if (result.exitCode == 0) {
            ArtifactResult.Success(
                target = target,
                artifactPath = "${outputPath}${getLibExtension(target)}",
                compilationDuration = duration,
                outputText = listOf(result.output, result.errors).joinToString("\n"),
                dependencyArtifacts = resolvedLibs,
            )
        } else {
            ArtifactResult.ProcessError(result.exitCode, result.errors.ifBlank { result.output })
        }
    }

    suspend fun buildAllBins(releaseMode: Boolean, target: KotlinTarget): List<ArtifactResult> {
        val dependencyTree = resolveDependencyTree(module, moduleFolder, listOf(target)).also {
            if (context.debug) it.printDependencyTree()
        }

        if (!outFolder.exists() && !outFolder.mkdirs()) {
            error("Could not create build folder: ${outFolder.getAbsolutePath()}")
        }

        val resolvedDeps = assembleDependencies(dependencyTree, releaseMode, target, true)
        val sourceFiles = collectSourceFiles(target, BuildType.BIN) // TODO: Scan only for main.kt and bin files
        val mainSource: File? = sourceFiles.mainFile?.run(::File)
        return listOfNotNull(
            if (mainSource?.exists() == true) {
                buildBin(releaseMode, module.name, target, resolvedDeps.artifacts)
            } else null,
        ) + sourceFiles.binFiles.map { otherBin ->
            val binFile = File(otherBin)
            buildBin(releaseMode, binFile.nameWithoutExtension, target, resolvedDeps.artifacts)
        }
    }

    suspend fun buildAll(releaseMode: Boolean, target: KotlinTarget): List<ArtifactResult> {
        return buildAllBins(releaseMode, target) + buildLib(releaseMode, target)
    }

    private suspend fun assembleDependencies(
        root: RootDependencyNode,
        releaseMode: Boolean,
        target: KotlinTarget,
        downloadArtifacts: Boolean,
    ): RootDependencyNode {
        val (newRoot, duration) = measureSeconds {
            val (mavenDependencies, mavenDuration) = measureSeconds {
                root.children
                    .filter { it.dependencyConf is DependencyConf.MavenDependency }
                    .map { child -> fetchMavenDependency(child, releaseMode, target, false) }
            }

            if (context.debug) {
                context.term.println("Assembled maven dependencies in ${mavenDuration}s")
            }

            val (localDependencies, localDuration) = measureSeconds {
                root.children
                    .filter { it.dependencyConf is DependencyConf.LocalPathDependency }
                    .map { child -> buildChildDependency(child, releaseMode, target, downloadArtifacts) }
            }

            if (context.debug) {
                context.term.println("Assembled local dependencies in ${localDuration}s")
            }
            root.copy(
                children = localDependencies + mavenDependencies,
                artifacts = localDependencies.flatMap { it.artifacts } + mavenDependencies.flatMap { it.artifacts },
            )
        }

        if (context.debug) {
            context.term.println("Assembled dependencies in ${duration}s")
        }

        return newRoot
    }

    private suspend fun buildChildDependency(
        child: ChildDependencyNode,
        releaseMode: Boolean,
        target: KotlinTarget,
        downloadArtifacts: Boolean,
        libs: List<String>? = null
    ): ChildDependencyNode {
        val childPath = "${basePath}${filePathSeparator}${child.localModule!!.name}"
        val childBuilder = ModuleBuilder(child.localModule, context, childPath)

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
        } else null

        return child.copy(
            children = innerDepNodes,
            artifacts = innerLibs + listOfNotNull(result)
        )
    }

    private suspend fun fetchMavenDependency(
        child: ChildDependencyNode,
        releaseMode: Boolean,
        target: KotlinTarget,
        downloadArtifacts: Boolean,
    ): ChildDependencyNode {
        val dependency = child.dependencyConf as DependencyConf.MavenDependency
        if (!downloadArtifacts && mavenDepCache.containsKey(dependency.toMavenString())) {
            // TODO: After resolving dependencies, reuse memory cache when downloading artifacts.
            return child
        }

        val artifactRemotePath = dependency.groupId.split('.')
            .plus(dependency.artifactId)
            .plus(dependency.version)
            .joinToString("/")
        val artifactModuleName = "${dependency.artifactId}-${dependency.version}.module"
        val artifactModuleCacheFile = cacheRoot
            .nestedFile(artifactRemotePath.replace('/', filePathSeparator))
            .nestedFile(artifactModuleName)

        if (context.debug) {
            println("Fetching maven dependency: ${dependency.toMavenString()}")
        }

        val gradleModule: GradleModule? =
            fetchGradleModule(artifactModuleCacheFile, artifactRemotePath, artifactModuleName)
        if (gradleModule == null) {
            return fetchPomDependency(dependency, artifactRemotePath, releaseMode, target, child, downloadArtifacts)
                .also { mavenDepCache[dependency.toMavenString()] = it }
        }
        val variant = if (target.isNative) {
            val knTarget = when (target) {
                KotlinTarget.JVM -> null
                KotlinTarget.JS_NODE -> null
                KotlinTarget.JS_BROWSER -> null
                KotlinTarget.MACOS_ARM64 -> "macos_arm64"
                KotlinTarget.MACOS_X64 -> "macos_x64"
                KotlinTarget.MINGW_X64 -> "mingw_x64"
                KotlinTarget.LINUX_X64 -> "linux_x64"
                KotlinTarget.LINUX_ARM64 -> "linux_arm64"
            }
            gradleModule.variants.firstOrNull { variant ->
                variant.attributes?.run {
                    orgJetbrainsKotlinPlatformType == "native" && orgJetbrainsKotlinNativeTarget == knTarget
                } ?: false
            }
        } else if (target == KotlinTarget.JVM) {
            gradleModule.variants.firstOrNull { variant ->
                variant.attributes?.run {
                    orgJetbrainsKotlinPlatformType == "jvm" && orgGradleLibraryelements == "jar"
                } ?: false
            }
        } else {
            gradleModule.variants.firstOrNull { variant ->
                variant.attributes?.run {
                    orgJetbrainsKotlinJsCompiler == "ir" && orgJetbrainsKotlinPlatformType == "js"
                } ?: false
            }
        }
        variant ?: error("Could not find variant for $target in ${dependency.toMavenString()}")

        val (targetVariant, files) = if (variant.availableAt == null) {
            variant to if (downloadArtifacts) {
                variant.files.map { file ->
                    fetchArtifactFromMetadata(file, dependency, dependency.artifactId, dependency.version)
                        .getAbsolutePath()
                }
            } else emptyList()
        } else {
            val artifactTargetModuleFileName = "${variant.availableAt.module}-${variant.availableAt.version}.module"
            val artifactTargetModuleRemotePath = dependency.groupId.split('.')
                .plus(variant.availableAt.module)
                .plus(variant.availableAt.version)
                .joinToString("/")
            val artifactTargetModuleCacheFile = cacheRoot
                .nestedFile(artifactTargetModuleRemotePath.replace('/', filePathSeparator))
                .nestedFile(artifactTargetModuleFileName)

            val targetGradleModule: GradleModule =
                fetchGradleModule(
                    artifactTargetModuleCacheFile,
                    artifactTargetModuleRemotePath,
                    artifactTargetModuleFileName
                ) ?: error("Expected to find artifact target module")
            val targetVariant = targetGradleModule.variants.first()
            val moduleName = variant.availableAt.module
            val version = variant.availableAt.version
            targetVariant to if (downloadArtifacts) {
                targetVariant.files.map { file ->
                    fetchArtifactFromMetadata(file, dependency, moduleName, version).getAbsolutePath()
                }
            } else emptyList()
        }

        val newChildDeps = targetVariant.dependencies
            .filter { !it.module.startsWith("kotlin-stdlib") && !it.module.endsWith("-bom") }
            .map { gradleDep ->
                val newNode = ChildDependencyNode(
                    localModule = null,
                    dependencyConf = DependencyConf.MavenDependency(
                        groupId = gradleDep.group,
                        artifactId = gradleDep.module,
                        version = gradleDep.version.requires,
                        scope = DependencyScope.IMPLEMENTATION,
                    ),
                    children = emptyList(),
                    artifacts = emptyList(),
                )
                fetchMavenDependency(newNode, releaseMode, target, downloadArtifacts)
            }

        return child.copy(
            children = newChildDeps,
            artifacts = (files + newChildDeps.flatMap { it.artifacts }).distinct(),
        ).also { mavenDepCache[dependency.toMavenString()] = it }
    }

    private suspend fun fetchPomDependency(
        dependency: DependencyConf.MavenDependency,
        artifactRemotePath: String,
        releaseMode: Boolean,
        target: KotlinTarget,
        child: ChildDependencyNode,
        downloadArtifacts: Boolean,
    ): ChildDependencyNode {
        val pomFileName = "${dependency.artifactId}-${dependency.version}.pom"
        val pomFileNameCacheFile = cacheRoot
            .nestedFile(artifactRemotePath.replace('/', filePathSeparator))
            .nestedFile(pomFileName)

        val pom: MavenProject = if (pomFileNameCacheFile.exists()) {
            xml.decodeFromString(pomFileNameCacheFile.readText())
        } else {
            val pomUrl = "${mavenRepoUrl.trimEnd('/')}/$artifactRemotePath/$pomFileName"
            val response = context.http.get(pomUrl)
            if (!response.status.isSuccess()) {
                context.term.println("${failed("Failed")} Could not find pom at $pomUrl")
                exitProcess(1)
            }

            val pomBody = response.bodyAsText()
            pomFileNameCacheFile.apply {
                getParentFile()?.mkdirs()
                createNewFile()
                writeText(pomBody)
            }

            xml.decodeFromString(pomBody)
        }

        val artifactFile = if (downloadArtifacts) fetchArtifact(dependency).getAbsolutePath() else null

        val childDeps = pom.dependencies
            .filter { it.scope?.value != "test" && it.version != null }
            .map { mavenDep ->
                val newChild = ChildDependencyNode(
                    localModule = null,
                    dependencyConf = DependencyConf.MavenDependency(
                        groupId = mavenDep.groupId.value,
                        artifactId = mavenDep.artifactId.value,
                        version = checkNotNull(mavenDep.version).value,
                        scope = when (mavenDep.scope?.value) {
                            "compile" -> DependencyScope.COMPILE
                            "runtime" -> DependencyScope.IMPLEMENTATION
                            else -> DependencyScope.IMPLEMENTATION
                        }
                    ),
                    children = emptyList(),
                    artifacts = emptyList(),
                )
                fetchMavenDependency(newChild, releaseMode, target, downloadArtifacts)
            }
        return child.copy(
            children = childDeps,
            artifacts = childDeps.flatMap { it.artifacts } + listOfNotNull(artifactFile),
        )
    }

    private suspend fun fetchArtifact(dependency: DependencyConf.MavenDependency): File {
        val actualArtifactName = "${dependency.artifactId}-${dependency.version}.jar"
        val actualArtifactPath = dependency.groupId.split('.')
            .plus(dependency.artifactId)
            .plus(dependency.version)
            .plus(actualArtifactName)
            .joinToString("/")
        return fetchMavenArtifact(actualArtifactPath)
    }

    private suspend fun fetchArtifactFromMetadata(
        targetFile: GradleModule.Variant.File,
        dependency: DependencyConf.MavenDependency,
        moduleName: String,
        version: String,
    ): File {
        val actualArtifactName = targetFile.url
        val actualArtifactPath = dependency.groupId.split('.')
            .plus(moduleName)
            .plus(version)
            .plus(actualArtifactName)
            .joinToString("/")
        return fetchMavenArtifact(actualArtifactPath)
    }

    private suspend fun fetchMavenArtifact(artifactPath: String): File {
        val cacheFile = cacheRoot.nestedFile(artifactPath.replace('/', filePathSeparator))

        return if (cacheFile.exists()) {
            cacheFile
        } else {
            val moduleUrl = "${mavenRepoUrl.trimEnd('/')}/$artifactPath"
            val response = context.http.get(moduleUrl)
            if (!response.status.isSuccess()) {
                context.term.println("${failed("Failed")} Could not find module at $moduleUrl")
                exitProcess(1)
            }
            cacheFile.apply {
                getParentFile()?.mkdirs()
                createNewFile()
                writeBytes(response.bodyAsChannel().toByteArray())
            }
        }
    }

    private suspend fun fetchGradleModule(
        artifactModuleCacheFile: File,
        artifactRemotePath: String,
        artifactModuleName: String
    ): GradleModule? = if (artifactModuleCacheFile.exists()) {
        json.decodeFromString(artifactModuleCacheFile.readText())
    } else {
        val moduleUrl = "${mavenRepoUrl.trimEnd('/')}/$artifactRemotePath/$artifactModuleName"
        val response = context.http.get(moduleUrl)
        if (response.status.isSuccess()) {
            val bodyText = response.bodyAsText()
            artifactModuleCacheFile.apply {
                getParentFile()?.mkdirs()
                createNewFile()
                writeText(bodyText)
            }
            json.decodeFromString(bodyText)
        } else {
            null
        }
    }

    suspend fun resolveDependencyTree(
        root: ModuleConf,
        rootFolder: File,
        targets: List<KotlinTarget>
    ): RootDependencyNode {
        val dependencies = root.dependencies
            .filter { it.targets.isEmpty() || (targets.isNotEmpty() && it.targets.containsAll(targets)) }
            .flatMap { it.dependencies }
            .map { dependency ->
                when (dependency) {
                    is DependencyConf.LocalPathDependency -> resolveLocalDependency(dependency, rootFolder, targets)
                    is DependencyConf.MavenDependency -> resolveMavenDependency(dependency, rootFolder, targets)
                    is DependencyConf.GitDependency -> TODO()
                    is DependencyConf.NpmDependency -> TODO()
                }
            }
        return RootDependencyNode(targets, root, dependencies)
    }

    suspend fun resolveMavenDependency(
        dependencyConf: DependencyConf.MavenDependency,
        rootFolder: File,
        targets: List<KotlinTarget>
    ): ChildDependencyNode {
        return fetchMavenDependency(
            ChildDependencyNode(
                localModule = null,
                dependencyConf = dependencyConf,
                children = emptyList(),
            ),
            releaseMode = false,
            target = targets.firstOrNull() ?: KotlinTarget.JVM, // TODO:
            downloadArtifacts = false,
        )
    }

    private suspend fun resolveLocalDependency(
        dependencyConf: DependencyConf.LocalPathDependency,
        rootFolder: File,
        targets: List<KotlinTarget>
    ): ChildDependencyNode {
        val packFile = rootFolder.nestedFile(dependencyConf.path).nestedFile(PACK_SCRIPT_FILENAME)
        val localModule = context.loadKtpackConf(packFile.getAbsolutePath()).module
        val children =
            resolveDependencyTree(localModule, rootFolder.nestedFile(dependencyConf.path), targets).children
        return ChildDependencyNode(
            localModule = localModule,
            dependencyConf = dependencyConf,
            children = children,
        )
    }

    private fun getExeExtension(target: KotlinTarget): String {
        return when (target) {
            KotlinTarget.JVM -> ".jar"
            KotlinTarget.MINGW_X64 -> ".exe"

            KotlinTarget.JS_NODE,
            KotlinTarget.JS_BROWSER -> ".js"

            KotlinTarget.MACOS_ARM64,
            KotlinTarget.MACOS_X64,
            KotlinTarget.LINUX_ARM64,
            KotlinTarget.LINUX_X64 -> ".kexe"
        }
    }

    private fun getLibExtension(target: KotlinTarget): String {
        return when (target) {
            KotlinTarget.JVM -> ".jar"
            KotlinTarget.JS_NODE,
            KotlinTarget.JS_BROWSER -> ""

            KotlinTarget.MINGW_X64,
            KotlinTarget.MACOS_ARM64,
            KotlinTarget.MACOS_X64,
            KotlinTarget.LINUX_ARM64,
            KotlinTarget.LINUX_X64 -> ".klib"
        }
    }

    private suspend fun startKotlinCompiler(
        mainSource: File?,
        sourceFiles: List<String>,
        releaseMode: Boolean,
        outputPath: String,
        target: KotlinTarget,
        isBinary: Boolean,
        libs: List<String>? = null,
    ): CommunicateResult = exec {
        val kotlinVersion = module.kotlinVersion ?: Ktpack.KOTLIN_VERSION
        when (target) {
            KotlinTarget.JVM -> {
                val targetOutPath = "${outputPath}${getExeExtension(target)}"
                arg(context.kotlinInstalls.findKotlincJvm(kotlinVersion))

                // arg("-Xjdk-release=$version")
                // args("-jvm-target", "1.8")
                arg("-include-runtime")
                // args("-jdk-home", "")

                if (!libs.isNullOrEmpty()) {
                    args("-classpath", libs.joinToString(CPSEP))
                }

                args("-d", targetOutPath) // output folder/ZIP/Jar path
            }

            KotlinTarget.JS_NODE, KotlinTarget.JS_BROWSER -> {
                val targetOutPath = "${outputPath}${getExeExtension(target)}"
                arg(context.kotlinInstalls.findKotlincJs(kotlinVersion))

                args("-output", targetOutPath) // output js file
                args("-main", if (isBinary) "call" else "noCall")

                if (!isBinary) {
                    arg("-meta-info")
                }
                // arg("-source-map")
                // args("-source-map-base-dirs", <path>)
                // args("-source-map-embed-sources", "never") // always|never|inlining
                // arg("-no-stdlib")

                args("-module-kind", "umd") // umd|commonjs|amd|plain

                if (!libs.isNullOrEmpty()) {
                    val jsLibFiles = libs
                        .filter { !it.endsWith(".jar") && !it.endsWith(".klib") }
                        .map { libPath -> File("$libPath.meta.js").getAbsolutePath() }
                    val archiveLibFiles = libs
                        .filter { it.endsWith(".jar") || it.endsWith(".klib") }
                    args("-libraries", (jsLibFiles + archiveLibFiles).joinToString(CPSEP))
                }
            }

            KotlinTarget.MACOS_ARM64,
            KotlinTarget.MACOS_X64,
            KotlinTarget.MINGW_X64,
            KotlinTarget.LINUX_ARM64,
            KotlinTarget.LINUX_X64 -> {
                val targetOutPath = if (isBinary) {
                    "${outputPath}${getExeExtension(target)}"
                } else {
                    outputPath // library suffix will be added be the compiler
                }
                arg(context.kotlinInstalls.findKotlincNative(kotlinVersion))
                File(targetOutPath.substringBeforeLast(filePathSeparator)).mkdirs()

                args("-output", targetOutPath) // output kexe or exe file
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

                libs?.forEach { lib ->
                    args("-library", lib)
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

        arg("-verbose")
        // arg("-nowarn")
        // arg("-Werror")

        arg("-Xmulti-platform")

        val serializationPlugin = File(context.kotlinInstalls.findKotlinHome(kotlinVersion), "lib")
            .nestedFile("kotlinx-serialization-compiler-plugin.jar")
        // arg("-Xplugin=${serializationPlugin.getAbsolutePath()}")

        // args("-kotlin-home", path)
        // args("-opt-in", <class>)

        mainSource?.getAbsolutePath()?.run(::arg)
        sourceFiles.forEach { file -> arg(file) }

        if (context.debug) {
            println("Launching Kotlin compiler: ")
            println(arguments.joinToString(" "))
        }
    }
}
