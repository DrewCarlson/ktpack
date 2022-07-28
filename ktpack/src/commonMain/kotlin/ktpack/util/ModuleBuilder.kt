package ktpack.util

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ksubprocess.*
import ktfio.*
import ktpack.*
import ktpack.commands.kotlin.KotlincInstalls
import ktpack.configuration.*
import ktpack.gradle.GradleModule
import kotlin.system.exitProcess

sealed class ArtifactResult {
    data class Success(
        val artifactPath: String,
        val compilationDuration: Double,
        val outputText: String,
        val target: Target,
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
    private val context: CliContext,
    private val basePath: String,
) {

    private val moduleFolder = File(basePath)
    val outFolder = moduleFolder.nestedFile("out")
    val srcFolder = moduleFolder.nestedFile("src")
    private val binFolder = srcFolder.nestedFile("bin")
    private val mainSource: File? = srcFolder.nestedFile("main.kt").takeIf(File::exists)

    private val otherBins: List<File>
        get() = if (binFolder.exists()) binFolder.listFiles().toList() else emptyList()

    // Collect other non-bin source files
    private val sourceFiles: List<File> by lazy {
        srcFolder
            .walkTopDown()
            .onEnter { it != binFolder }
            .filter { file -> file.getName().endsWith(".kt") && file != mainSource }
            .toList()
    }

    suspend fun buildBin(
        releaseMode: Boolean,
        binName: String,
        target: Target,
        libs: List<String>? = null,
    ): ArtifactResult = Dispatchers.Default {
        check(srcFolder.isDirectory()) { "Expected directory at ${srcFolder.getAbsolutePath()}" }
        val dependencyTree = resolveDependencyTree(module, moduleFolder, listOf(target))

        val mainSource = mainSource

        val selectedBinFile = if (binName == module.name) {
            mainSource
        } else {
            otherBins.find { it.nameWithoutExtension == binName }
        } ?: return@Default ArtifactResult.NoArtifactFound

        val modeString = if (releaseMode) "release" else "debug"
        val targetBinDir = File(outFolder.getAbsolutePath(), target.name.lowercase(), modeString, "bin")
        if (!targetBinDir.exists()) targetBinDir.mkdirs()
        val outputPath = targetBinDir.nestedFile(binName).getAbsolutePath()
        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, releaseMode, target).artifacts
        val (result, duration) = measureSeconds {
            startKotlinCompiler(selectedBinFile, sourceFiles, releaseMode, outputPath, target, true, resolvedLibs)
        }
        return@Default if (result.exitCode == 0) {
            ArtifactResult.Success(
                target = target,
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
        target: Target,
        libs: List<String>? = null,
    ): ArtifactResult = Dispatchers.Default {
        check(srcFolder.isDirectory()) { "Expected directory at ${srcFolder.getAbsolutePath()}" }
        if (sourceFiles.isEmpty()) {
            return@Default ArtifactResult.NoArtifactFound
        }
        val dependencyTree = resolveDependencyTree(module, moduleFolder, listOf(target))

        val modeString = if (releaseMode) "release" else "debug"
        val targetLibDir = File(outFolder.getAbsolutePath(), target.name.lowercase(), modeString, "lib")
        if (!targetLibDir.exists()) targetLibDir.mkdirs()

        val resolvedLibs = libs ?: assembleDependencies(dependencyTree, releaseMode, target).artifacts
        val outputPath = listOf(targetLibDir.getAbsolutePath(), module.name)
            .joinToString(filePathSeparator.toString())
        val (result, duration) = measureSeconds {
            startKotlinCompiler(null, sourceFiles, releaseMode, outputPath, target, false, resolvedLibs)
        }
        return@Default if (result.exitCode == 0) {
            ArtifactResult.Success(
                target = target,
                artifactPath = "$outputPath.${getLibExtension(target)}",
                compilationDuration = duration,
                outputText = listOf(result.output, result.errors).joinToString("\n"),
            )
        } else {
            ArtifactResult.ProcessError(result.exitCode, result.errors.ifBlank { result.output })
        }
    }

    suspend fun buildAllBins(releaseMode: Boolean, target: Target): List<ArtifactResult> {
        check(srcFolder.isDirectory()) { "Expected directory at ${srcFolder.getAbsolutePath()}" }
        val dependencyTree = resolveDependencyTree(module, moduleFolder, listOf(target)).also {
            if (context.debug) it.printDependencyTree()
        }

        if (!outFolder.exists() && !outFolder.mkdirs()) {
            error("Could not create build folder: ${outFolder.getAbsolutePath()}")
        }

        val resolvedDeps = assembleDependencies(dependencyTree, releaseMode, target)

        val mainSource = mainSource
        return listOfNotNull(
            if (mainSource?.exists() == true) {
                buildBin(releaseMode, module.name, target, resolvedDeps.artifacts)
            } else null,
        ) + otherBins.map { otherBin ->
            buildBin(releaseMode, otherBin.nameWithoutExtension, target, resolvedDeps.artifacts)
        }
    }

    suspend fun buildAll(releaseMode: Boolean, target: Target): List<ArtifactResult> {
        return buildAllBins(releaseMode, target) + buildLib(releaseMode, target)
    }

    private suspend fun assembleDependencies(
        root: RootDependencyNode,
        releaseMode: Boolean,
        target: Target,
    ): RootDependencyNode {
        val localDependencies = root.children
            .filter { it.ktpackDependency is KtpackDependency.LocalPathDependency }
            .map { child -> buildChildDependency(child, releaseMode, target) }
        val mavenDependencies = root.children
            .filter { it.ktpackDependency is KtpackDependency.MavenDependency }
            .map { child -> fetchMavenDependency(child, releaseMode, target) }
        return root.copy(
            children = localDependencies + mavenDependencies,
            artifacts = localDependencies.flatMap { it.artifacts } + mavenDependencies.flatMap { it.artifacts },
        )
    }

    private suspend fun buildChildDependency(
        child: ChildDependencyNode,
        releaseMode: Boolean,
        target: Target,
        libs: List<String>? = null
    ): ChildDependencyNode {
        val childPath = "${basePath}${filePathSeparator}${child.localModule!!.name}"
        val childBuilder = ModuleBuilder(child.localModule, context, childPath)

        val innerDepNodes = child.children
            .filter { it.ktpackDependency is KtpackDependency.LocalPathDependency }
            .map { innerChild ->
                childBuilder.buildChildDependency(innerChild, releaseMode, target)
            }

        val innerLibs = innerDepNodes.flatMap { it.artifacts } + libs.orEmpty()
        val result = when (val result = childBuilder.buildLib(releaseMode, target, innerLibs)) {
            is ArtifactResult.Success -> result.artifactPath
            ArtifactResult.NoArtifactFound -> error("No artifact found at $childPath")
            is ArtifactResult.ProcessError -> error(result.message.orEmpty())
        }

        return child.copy(
            children = innerDepNodes,
            artifacts = innerLibs + result
        )
    }

    val mavenRepoUrl = "https://repo1.maven.org/maven2"

    private suspend fun fetchMavenDependency(
        child: ChildDependencyNode,
        releaseMode: Boolean,
        target: Target,
    ): ChildDependencyNode {
        val cacheRoot = File(KTPACK_ROOT, "maven-cache")
        val dependency = child.ktpackDependency as KtpackDependency.MavenDependency
        val artifactRemotePath = dependency.groupId.split('.')
            .plus(dependency.artifactId)
            .plus(dependency.version)
            .joinToString("/")
        val artifactModuleName = "${dependency.artifactId}-${dependency.version}.module"
        val artifactModuleCacheFile = cacheRoot
            .nestedFile(artifactRemotePath.replace('/', filePathSeparator))
            .nestedFile(artifactModuleName)

        val gradleModule: GradleModule =
            fetchGradleModule(artifactModuleCacheFile, artifactRemotePath, artifactModuleName)
        val variant = if (target.isNative) {
            val knTarget = when (target) {
                Target.JVM -> null
                Target.JS_NODE -> null
                Target.JS_BROWSER -> null
                Target.MACOS_ARM64 -> "macos_arm64"
                Target.MACOS_X64 -> "macos_x64"
                Target.WINDOWS_X64 -> "mingw_x64"
                Target.LINUX_X64 -> "linux_x64"
            }
            gradleModule.variants.firstOrNull { variant ->
                variant.attributes?.orgJetbrainsKotlinPlatformType == "native" &&
                        variant.attributes.orgJetbrainsKotlinNativeTarget == knTarget
            }
        } else if (target == Target.JVM) {
            gradleModule.variants.firstOrNull { variant ->
                variant.attributes?.orgJetbrainsKotlinPlatformType == "jvm" &&
                        variant.attributes.orgGradleLibraryelements == "jar"
            }
        } else {
            gradleModule.variants.firstOrNull { variant ->
                variant.attributes?.orgJetbrainsKotlinJsCompiler == "ir" &&
                        variant.attributes.orgJetbrainsKotlinPlatformType == "js"
            }
        }
        variant ?: error("Could not find variant for $target in $artifactModuleName")

        val artifactTargetModuleFileName = "${variant.availableAt?.module}-${variant.availableAt?.version}.module"
        val artifactTargetModuleRemotePath = dependency.groupId.split('.')
            .plus(variant.availableAt?.module ?: error("Variant issue: $variant"))
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
            )

        val targetVariant = targetGradleModule.variants.first()
        val files = targetVariant.files.map { file ->
            fetchArtifact(file, dependency, variant.availableAt, cacheRoot).getAbsolutePath()
        }

        val newChildDeps = targetVariant.dependencies
            .filter { !it.module.startsWith("kotlin-stdlib") }
            .map { gradleDep ->
                val newNode = ChildDependencyNode(
                    localModule = null,
                    ktpackDependency = KtpackDependency.MavenDependency(
                        groupId = gradleDep.group,
                        artifactId = gradleDep.module,
                        version = gradleDep.version.requires,
                        scope = DependencyScope.IMPLEMENTATION,
                    ),
                    children = emptyList(),
                    artifacts = emptyList(),
                )
                fetchMavenDependency(newNode, releaseMode, target)
            }

        return child.copy(
            children = newChildDeps,
            artifacts = files + newChildDeps.flatMap { it.artifacts },
        )
    }

    private suspend fun fetchArtifact(
        targetFile: GradleModule.Variant.File,
        dependency: KtpackDependency.MavenDependency,
        availableAt: GradleModule.Variant.AvailableAt,
        cacheRoot: File
    ): File {
        val actualArtifactName = targetFile.url
        val actualArtifactPath = dependency.groupId.split('.')
            .plus(availableAt.module)
            .plus(availableAt.version)
            .plus(actualArtifactName)
            .joinToString("/")
        val actualArtifactCacheFile = cacheRoot.nestedFile(actualArtifactPath.replace('/', filePathSeparator))

        if (!actualArtifactCacheFile.exists()) {
            val moduleUrl = "${mavenRepoUrl.trimEnd('/')}/$actualArtifactPath"
            val response = context.http.get(moduleUrl)
            if (!response.status.isSuccess()) {
                context.term.println("${failed("Failed")} Could not find module at $moduleUrl")
                exitProcess(1)
            }

            actualArtifactCacheFile.apply {
                getParentFile()?.mkdirs()
                createNewFile()
                writeBytes(response.bodyAsChannel().toByteArray())
            }
        }
        return actualArtifactCacheFile
    }

    private suspend fun fetchGradleModule(
        artifactModuleCacheFile: File,
        artifactRemotePath: String,
        artifactModuleName: String
    ): GradleModule = if (artifactModuleCacheFile.exists()) {
        Json.decodeFromString(artifactModuleCacheFile.readText())
    } else {
        val moduleUrl = "${mavenRepoUrl.trimEnd('/')}/$artifactRemotePath/$artifactModuleName"
        val response = context.http.get(moduleUrl)
        if (!response.status.isSuccess()) {
            context.term.println("${failed("Failed")} Could not find module at $moduleUrl")
            exitProcess(1)
        }
        val bodyText = response.bodyAsText()
        artifactModuleCacheFile.apply {
            getParentFile()?.mkdirs()
            createNewFile()
            writeText(bodyText)
        }
        Json.decodeFromString(bodyText)
    }


    suspend fun resolveDependencyTree(root: ModuleConf, rootFolder: File, targets: List<Target>): RootDependencyNode {
        val dependencies = root.dependencies
            .filter { it.targets.isEmpty() || (targets.isNotEmpty() && it.targets.containsAll(targets)) }
            .flatMap { it.dependencies }
            .map { dependency ->
                when (dependency) {
                    is KtpackDependency.LocalPathDependency -> resolveLocalDependency(dependency, rootFolder, targets)
                    is KtpackDependency.MavenDependency -> resolveMavenDependency(dependency, rootFolder, targets)
                    is KtpackDependency.GitDependency -> TODO()
                    is KtpackDependency.NpmDependency -> TODO()
                }
            }
        return RootDependencyNode(targets, root, dependencies)
    }

    suspend fun resolveMavenDependency(
        ktpackDependency: KtpackDependency.MavenDependency,
        rootFolder: File,
        targets: List<Target>
    ): ChildDependencyNode {

        return ChildDependencyNode(
            localModule = null,
            ktpackDependency = ktpackDependency,
            children = emptyList(),
        )
    }

    private suspend fun resolveLocalDependency(
        ktpackDependency: KtpackDependency.LocalPathDependency,
        rootFolder: File,
        targets: List<Target>
    ): ChildDependencyNode {
        val manifest = rootFolder.nestedFile(ktpackDependency.path).nestedFile(MANIFEST_NAME)
        val localModule = loadManifest(context, manifest.getAbsolutePath()).module
        val children =
            resolveDependencyTree(localModule, rootFolder.nestedFile(ktpackDependency.path), targets).children
        return ChildDependencyNode(
            localModule = localModule,
            ktpackDependency = ktpackDependency,
            children = children,
        )
    }

    private fun getExeExtension(target: Target): String {
        return when (target) {
            Target.JVM -> "jar"
            Target.WINDOWS_X64 -> "exe"
            Target.JS_NODE,
            Target.JS_BROWSER -> "js"

            Target.MACOS_ARM64,
            Target.MACOS_X64,
            Target.LINUX_X64 -> "kexe"
        }
    }

    private fun getLibExtension(target: Target): String {
        return when (target) {
            Target.JVM -> "jar"
            Target.JS_NODE,
            Target.JS_BROWSER -> "js"

            Target.WINDOWS_X64,
            Target.MACOS_ARM64,
            Target.MACOS_X64,
            Target.LINUX_X64 -> "klib"
        }
    }

    private suspend fun startKotlinCompiler(
        mainSource: File?,
        sourceFiles: List<File>,
        releaseMode: Boolean,
        outputPath: String,
        target: Target,
        isBinary: Boolean,
        libs: List<String>? = null,
    ): CommunicateResult = exec {
        val kotlinVersion = module.kotlinVersion ?: Ktpack.KOTLIN_VERSION

        when (target) {
            Target.JVM -> {
                val targetOutPath = "${outputPath}.${getExeExtension(target)}"
                arg(KotlincInstalls.findKotlincJvm(kotlinVersion))

                args("-d", targetOutPath) // output folder/ZIP/Jar path

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
                args("-main", if (isBinary) "call" else "noCall")

                if (!isBinary) {
                    arg("-meta-info")
                }
                // arg("-source-map")
                // args("-source-map-base-dirs", <path>)
                // args("-source-map-embed-sources", "never") // always|never|inlining
                // arg("-no-stdlib")

                args("-module-kind", "umd") // umd|commonjs|amd|plain
            }

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

                val konanTarget = when (target) {
                    Target.WINDOWS_X64 -> "mingw_x64"
                    else -> target.name.lowercase()
                }
                args("-target", konanTarget)
            }
        }

        mainSource?.getAbsolutePath()?.run(::arg)
        sourceFiles.forEach { file ->
            arg(file.getAbsolutePath())
        }
        arg("-verbose")
        // arg("-nowarn")
        // arg("-Werror")

        arg("-Xmulti-platform")

        //arg("-Xplugin=\$KOTLIN_HOME/lib/kotlinx-serialization-compiler-plugin.jar")

        // args("-kotlin-home", path)
        // args("-opt-in", <class>)

        if (context.debug) {
            println("Launching Kotlin compiler: ")
            println(arguments.joinToString(" "))
        }
    }
}

data class RootDependencyNode(
    val targets: List<Target>,
    val module: ModuleConf,
    val children: List<ChildDependencyNode>,
    val artifacts: List<String> = emptyList(),
) {
    fun printDependencyTree() {
        println("Dependencies for '${module.name}' on ${targets.joinToString().ifBlank { "all targets" }}:")
        children.forEach { child ->
            printChild(child, 1)
        }
    }

    private fun printChild(child: ChildDependencyNode, level: Int) {
        repeat(level) { print("--") }
        println(" $child")
        child.children.forEach { innerChild ->
            printChild(innerChild, level + 1)
        }
    }
}

data class ChildDependencyNode(
    val localModule: ModuleConf?,
    val ktpackDependency: KtpackDependency,
    val children: List<ChildDependencyNode>,
    val artifacts: List<String> = emptyList(),
) {
    override fun toString(): String = when (ktpackDependency) {
        is KtpackDependency.LocalPathDependency -> "local: module=${localModule?.name} path=${ktpackDependency.path}"
        is KtpackDependency.GitDependency -> "git: module=${localModule?.name} url=${ktpackDependency.gitUrl}"
        is KtpackDependency.MavenDependency -> "maven: ${ktpackDependency.artifactId}"
        is KtpackDependency.NpmDependency -> "npm: name=${ktpackDependency.name} dev=${ktpackDependency.isDev}"
    }
}

