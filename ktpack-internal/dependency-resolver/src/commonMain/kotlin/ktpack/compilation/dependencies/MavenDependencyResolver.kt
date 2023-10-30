package ktpack.compilation.dependencies

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.decodeFromString
import ktpack.compilation.dependencies.models.DependencyNode
import ktpack.configuration.*
import ktpack.gradle.GradleModule
import ktpack.json
import ktpack.maven.MavenProject
import ktpack.util.*
import ktpack.xml
import okio.Path
import okio.Path.Companion.DIRECTORY_SEPARATOR

class MavenDependencyResolver(
    override val module: ModuleConf,
    private val http: HttpClient,
) : DependencyResolver() {
    // TODO: Support list of maven urls and local repo folders
    private val mavenRepoUrl = "https://repo1.maven.org/maven2"
    private val cacheRoot = pathFrom(KTPACK_ROOT, "maven-cache")
    private val logger = Logger.withTag(MavenDependencyResolver::class.simpleName.orEmpty())

    private val nodeCache = mutableMapOf<String, DependencyNode>()
    private val gradleModuleCache = mutableMapOf<String, GradleModule>()
    private val mavenProjectCache = mutableMapOf<String, MavenProject>()

    override fun canResolve(node: DependencyNode): Boolean {
        return node.dependencyConf is DependencyConf.MavenDependency
    }

    override suspend fun resolveArtifacts(
        nodes: List<DependencyNode>,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): List<DependencyNode> {
        logger.d { "Resolving artifacts [$target] (${if (releaseMode) "release" else "debug"})" }
        nodes.forEach { logger.d(it.toString()) }
        return nodes.mapNotNull { node ->
            val dependency = node.dependencyConf as DependencyConf.MavenDependency
            val cachedGradleModule = readCachedGradleModule(dependency)
            if (cachedGradleModule != null) {
                val (variant, files) = downloadVariantArtifacts(target, cachedGradleModule.variants, dependency)
                logger.d { "Artifact selected: Gradle Module Variant ${variant.name}\n${files.joinToString("\n")}" }
                return@mapNotNull node.copy(artifacts = files)
            }

            val cachedPom = readCachedPom(dependency)
            if (cachedPom != null) {
                val artifactName = "${dependency.artifactId}-${dependency.version}.jar"
                val artifactPath = (dependency.toPathParts() + artifactName).joinToString("/")
                val artifactFile = fetchPomArtifact(artifactPath).takeIf { it.exists() }?.toString()
                logger.d { "Artifact select: Maven POM found with $artifactFile" }
                return@mapNotNull node.copy(artifacts = listOfNotNull(artifactFile))
            }

            return@mapNotNull null
        }
    }

    private suspend fun loadLocalDependencyNode(
        dependency: DependencyConf.MavenDependency,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): DependencyNode? {
        // Check for local gradle module data
        val cachedGradleModule = readCachedGradleModule(dependency)
        if (cachedGradleModule != null) {
            val targetVariant = resolveGradleModuleVariant(target, cachedGradleModule.variants, dependency)
            return DependencyNode(
                localModule = null,
                dependencyConf = dependency,
                children = resolveGradleVariantDependencies(targetVariant, releaseMode, target),
                artifacts = emptyList(),
            ).also { nodeCache[dependency.toMavenString()] = it }
        }


        val cachedPom = readCachedPom(dependency)
        if (cachedPom != null) {
            return DependencyNode(
                localModule = null,
                dependencyConf = dependency,
                children = parseChildDependencies(cachedPom, releaseMode, target),
                artifacts = emptyList(),
            ).also { nodeCache[dependency.toMavenString()] = it }
        }

        return null
    }

    override suspend fun resolve(
        node: DependencyNode,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): DependencyNode {
        val dependency = node.dependencyConf as DependencyConf.MavenDependency
        val cachedNode = nodeCache[dependency.toMavenString()]
        if (cachedNode != null) {
            return cachedNode
        }
        val localNode = loadLocalDependencyNode(dependency, releaseMode, target)
        if (localNode != null) {
            return localNode
        }

        val artifactRemotePath = dependency.toPathString("/")
        val artifactModuleName = "${dependency.artifactId}-${dependency.version}.module"
        val gradleModule = fetchGradleModule(artifactRemotePath, artifactModuleName)
        if (gradleModule != null) {
            val targetVariant = resolveGradleModuleVariant(target, gradleModule.variants, dependency)
            val newChildDeps = resolveGradleVariantDependencies(targetVariant, releaseMode, target)
            return node.copy(
                children = newChildDeps,
                artifacts = emptyList(),
            ).also { nodeCache[dependency.toMavenString()] = it }
        }

        // No gradle module detected, try for pom
        return fetchPomDependency(
            dependency,
            releaseMode,
            target,
            node,
            false,
        ).also {
            nodeCache[dependency.toMavenString()] = it
        }
    }

    private suspend fun resolveGradleVariantDependencies(
        targetVariant: GradleModule.Variant,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): List<DependencyNode> {
        return targetVariant.dependencies
            // TODO: Explicit stdlib version handling
            .filter { !it.module.startsWith("kotlin-stdlib") && !it.module.endsWith("-bom") }
            .map { gradleDep ->
                val newNode = DependencyNode(
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
                resolve(newNode, releaseMode, target)
            }
    }

    private suspend fun fetchPomDependency(
        dependency: DependencyConf.MavenDependency,
        releaseMode: Boolean,
        target: KotlinTarget,
        child: DependencyNode,
        downloadArtifacts: Boolean,
    ): DependencyNode {
        val pom: MavenProject = fetchPomFile(dependency)

        val artifactName = "${dependency.artifactId}-${dependency.version}.jar"
        val artifactPath = (dependency.toPathParts() + artifactName).joinToString("/")

        val artifactFile = if (downloadArtifacts) {
            fetchPomArtifact(artifactPath).takeIf { it.exists() }
        } else {
            null
        }

        val childDeps = parseChildDependencies(pom, releaseMode, target)
        return child.copy(
            children = childDeps,
            artifacts = listOfNotNull(artifactFile?.toString()),
        )
    }

    private fun readCachedPom(dependency: DependencyConf.MavenDependency): MavenProject? {
        val pomPath = dependency.toPathString(DIRECTORY_SEPARATOR)
        val pomFileName = "${dependency.artifactId}-${dependency.version}.pom"
        val pomFileNameCacheFile = cacheRoot / pomPath / pomFileName

        return if (pomFileNameCacheFile.exists()) {
            logger.d { "Found cached POM for '${dependency.toMavenString()}': $pomFileNameCacheFile" }
            xml.decodeFromString(pomFileNameCacheFile.readUtf8())
        } else {
            null
        }
    }

    private suspend fun fetchPomFile(
        dependency: DependencyConf.MavenDependency,
    ): MavenProject {
        val artifactRemotePath = dependency.toPathString("/")
        val cached = mavenProjectCache[artifactRemotePath]
        if (cached != null) {
            return cached
        }

        val pomFileName = "${dependency.artifactId}-${dependency.version}.pom"
        val cachedPomFile = readCachedPom(dependency)
        if (cachedPomFile != null) {
            return cachedPomFile
        }

        val pomUrl = "$mavenRepoUrl/$artifactRemotePath/$pomFileName"
        val response = http.get(pomUrl)
        if (!response.status.isSuccess()) {
            error("Failed (${response.status}): Could not find pom at $pomUrl")
        }

        val pomBody = response.bodyAsText()
        val pomFilePath = artifactRemotePath.urlAsPath()
        val pomFileNameCacheFile = cacheRoot / pomFilePath / pomFileName
        pomFileNameCacheFile.apply {
            parent?.mkdirs()
            createNewFile()
            writeUtf8(pomBody) { error ->
                throw error
            }
        }

        return xml.decodeFromString<MavenProject>(pomBody).also {
            mavenProjectCache[artifactRemotePath] = it
        }
    }

    private suspend fun parseChildDependencies(
        pom: MavenProject,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): List<DependencyNode> {
        return pom.dependencies
            .filter { it.scope?.value != "test" && it.version != null }
            .map { mavenDep ->
                val depConf = DependencyConf.MavenDependency(
                    groupId = mavenDep.groupId.value,
                    artifactId = mavenDep.artifactId.value,
                    version = checkNotNull(mavenDep.version).value,
                    scope = when (mavenDep.scope?.value) {
                        "compile" -> DependencyScope.COMPILE
                        "runtime" -> DependencyScope.IMPLEMENTATION
                        else -> DependencyScope.IMPLEMENTATION
                    },
                )
                val newChild = DependencyNode(
                    localModule = null,
                    dependencyConf = depConf,
                    children = emptyList(),
                    artifacts = emptyList(),
                )
                resolve(newChild, releaseMode, target).also { node ->
                    nodeCache[depConf.toMavenString()] = node
                }
            }
    }

    private suspend fun fetchPomArtifact(artifactUrlPath: String, overrideName: String? = null): Path {
        val cacheFile = if (overrideName == null) {
            cacheRoot / artifactUrlPath.urlAsPath()
        } else {
            // NOTE: Native targets have their klib file names without the target and version name.
            // This is not how gradle stores them on disk, but it is what the metadata defines.
            (cacheRoot / artifactUrlPath.urlAsPath()).parent!! / overrideName
        }
        if (cacheFile.exists()) {
            return cacheFile
        }
        val moduleUrl = "$mavenRepoUrl/$artifactUrlPath"
        val response = http.get(moduleUrl)
        if (!response.status.isSuccess()) {
            error("Failed: Could not find module at $moduleUrl")
        }
        return cacheFile.apply {
            parent?.mkdirs()
            createNewFile()
            writeBytes(response.bodyAsChannel().toByteArray()) { error ->
                throw error
            }
        }
    }

    private fun readCachedGradleModule(
        dependency: DependencyConf.MavenDependency,
    ): GradleModule? {
        val artifactRemotePath = dependency.toPathString("/")
        val artifactModuleName = "${dependency.artifactId}-${dependency.version}.module"
        return readCachedGradleModule(artifactModuleName, artifactRemotePath)
    }

    private fun readCachedGradleModule(
        artifactModuleName: String,
        artifactRemotePath: String,
    ): GradleModule? {
        val cached = gradleModuleCache[artifactRemotePath]
        if (cached != null) {
            return cached
        }
        val artifactModuleCacheFile = cacheRoot / artifactRemotePath.urlAsPath() / artifactModuleName
        return if (artifactModuleCacheFile.exists()) {
            logger.d { "Read cached Gradle module: $artifactModuleCacheFile" }
            json.decodeFromString<GradleModule?>(artifactModuleCacheFile.readUtf8())
                ?.also { gradleModuleCache[artifactRemotePath] = it }
        } else {
            null
        }
    }

    private suspend fun fetchGradleModule(
        artifactRemotePath: String,
        artifactName: String,
    ): GradleModule? {
        val cached = readCachedGradleModule(artifactName, artifactRemotePath)
        if (cached != null) {
            return cached
        }
        val moduleUrl = "$mavenRepoUrl/$artifactRemotePath/$artifactName"
        val response = http.get(moduleUrl)
        if (response.status.value == 404) {
            logger.w { "Gradle module not found: $moduleUrl" }
            return null
        }
        if (!response.status.isSuccess()) {
            logger.e { "Failed to fetch Gradle module: $moduleUrl" }
            return null
        }
        val bodyText = response.bodyAsText()
        val artifactModuleCacheFile = cacheRoot / artifactRemotePath.urlAsPath() / artifactName
        artifactModuleCacheFile.apply {
            parent?.mkdirs()
            createNewFile()
            writeUtf8(bodyText) { throw it }
        }
        return json.decodeFromString<GradleModule>(bodyText)
            .also { gradleModuleCache[artifactRemotePath] = it }
    }

    private suspend fun fetchGradleModuleArtifact(
        targetFile: GradleModule.Variant.File,
        dependency: DependencyConf.MavenDependency,
        moduleName: String,
        version: String,
    ): Path {
        val actualArtifactName = targetFile.url
        val actualArtifactPath = dependency.groupId.split('.')
            .plus(moduleName)
            .plus(version)
            .plus(actualArtifactName)
            .joinToString("/")
        return fetchPomArtifact(actualArtifactPath, targetFile.name)
    }


    private suspend fun downloadVariantArtifacts(
        target: KotlinTarget,
        variants: List<GradleModule.Variant>,
        dependency: DependencyConf.MavenDependency,
    ): Pair<GradleModule.Variant, List<String>> {
        val variant = variants.findVariantFor(target)
        val availableAt = variant.availableAt
        return if (availableAt == null) {
            variant to variant.files.mapNotNull { file ->
                fetchGradleModuleArtifact(
                    file,
                    dependency,
                    dependency.artifactId,
                    dependency.version,
                ).takeIf { it.exists() }
                    ?.toString()
            }
        } else {
            followVariantRedirect(availableAt, dependency, target, true)
        }
    }

    private suspend fun resolveGradleModuleVariant(
        target: KotlinTarget,
        variants: List<GradleModule.Variant>,
        dependency: DependencyConf.MavenDependency,
    ): GradleModule.Variant {
        val variant = variants.findVariantFor(target)
        val availableAt = variant.availableAt
        return if (availableAt == null) {
            variant
        } else {
            followVariantRedirect(availableAt, dependency, target, false).first
        }
    }

    private suspend fun followVariantRedirect(
        availableAt: GradleModule.Variant.AvailableAt,
        dependency: DependencyConf.MavenDependency,
        target: KotlinTarget,
        downloadArtifacts: Boolean,
    ): Pair<GradleModule.Variant, List<String>> {
        val artifactRemotePath =
            dependency.groupId.split('.')
                .plus(availableAt.module)
                .plus(availableAt.version)
                .joinToString("/")
        val artifactName = "${availableAt.module}-${availableAt.version}.module"
        val targetGradleModule = checkNotNull(fetchGradleModule(artifactRemotePath, artifactName)) {
            "Expected to find artifact target module at $artifactRemotePath"
        }
        val targetVariant = targetGradleModule.variants.findVariantFor(target)
        val moduleName = availableAt.module
        val version = availableAt.version
        val artifacts = if (downloadArtifacts) {
            targetVariant.files.mapNotNull { file ->
                fetchGradleModuleArtifact(file, dependency, moduleName, version)
                    .takeIf { it.exists() }
                    ?.toString()
            }
        } else {
            emptyList()
        }
        return targetVariant to artifacts
    }

    private fun String.urlAsPath(): String {
        return replace("/", DIRECTORY_SEPARATOR)
    }
}
