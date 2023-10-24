package ktpack.compilation.dependencies

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.decodeFromString
import ktfio.*
import ktpack.compilation.dependencies.models.ChildDependencyNode
import ktpack.configuration.DependencyConf
import ktpack.configuration.DependencyScope
import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf
import ktpack.gradle.GradleModule
import ktpack.json
import ktpack.maven.MavenProject
import ktpack.util.KTPACK_ROOT
import ktpack.xml
import okio.Path

class MavenDependencyResolver(
    override val module: ModuleConf,
    private val http: HttpClient,
) : DependencyResolver() {
    private val mavenRepoUrl = "https://repo1.maven.org/maven2"

    private val mavenDepCache = mutableMapOf<String, ChildDependencyNode>()

    private val cacheRoot = File(KTPACK_ROOT, "maven-cache")

    override fun canResolve(node: ChildDependencyNode): Boolean {
        return node.dependencyConf is DependencyConf.MavenDependency
    }

    override suspend fun resolve(
        node: ChildDependencyNode,
        releaseMode: Boolean,
        target: KotlinTarget,
        downloadArtifacts: Boolean,
        recurse: Boolean,
    ): ChildDependencyNode {
        val dependency = node.dependencyConf as DependencyConf.MavenDependency
        val artifactRemotePath = dependency.toPathString("/")

        // if (context.debug) {
        //    println("Fetching maven dependency: ${dependency.toMavenString()}")
        // }

        val gradleModule = fetchGradleModule(dependency, artifactRemotePath)
        if (gradleModule == null) {
            return fetchPomDependency(
                dependency,
                artifactRemotePath,
                releaseMode,
                target,
                node,
                downloadArtifacts,
                recurse,
            ).also {
                mavenDepCache[dependency.toMavenString()] = it
            }
        }

        val (targetVariant, files) = downloadVariantArtifacts(target, gradleModule.variants, downloadArtifacts, dependency)

        val newChildDeps = if (recurse) {
            targetVariant.dependencies
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
                    resolve(newNode, releaseMode, target, downloadArtifacts, recurse = true)
                }
        } else {
            emptyList()
        }

        return node.copy(
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
        recurse: Boolean,
    ): ChildDependencyNode {
        val pom: MavenProject = fetchPom(artifactRemotePath, dependency)

        val artifactFile = if (downloadArtifacts) fetchArtifactFromPom(dependency).getAbsolutePath() else null

        val childDeps = if (recurse) {
            parseChildDependencies(pom, releaseMode, target, downloadArtifacts)
        } else {
            emptyList()
        }
        return child.copy(
            children = childDeps,
            artifacts = childDeps.flatMap { it.artifacts } + listOfNotNull(artifactFile),
        )
    }

    private suspend fun fetchPom(
        artifactRemotePath: String,
        dependency: DependencyConf.MavenDependency,
    ): MavenProject {
        val pomFileName = "${dependency.artifactId}-${dependency.version}.pom"
        val pomFileNameCacheFile = cacheRoot
            .nestedFile(artifactRemotePath.replace('/', filePathSeparator))
            .nestedFile(pomFileName)

        if (pomFileNameCacheFile.exists()) {
            return xml.decodeFromString(pomFileNameCacheFile.readText())
        }

        val pomUrl = "${mavenRepoUrl.trimEnd('/')}/$artifactRemotePath/$pomFileName"
        val response = http.get(pomUrl)
        if (!response.status.isSuccess()) {
            // TODO: context.term.println("${failed("Failed")} Could not find pom at $pomUrl")
            error("Failed: Could not find pom at $pomUrl")
        }

        val pomBody = response.bodyAsText()
        pomFileNameCacheFile.apply {
            getParentFile()?.mkdirs()
            createNewFile()
            writeText(pomBody)
        }

        return xml.decodeFromString(pomBody)
    }

    private suspend fun parseChildDependencies(
        pom: MavenProject,
        releaseMode: Boolean,
        target: KotlinTarget,
        downloadArtifacts: Boolean,
    ): List<ChildDependencyNode> {
        return pom.dependencies
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
                        },
                    ),
                    children = emptyList(),
                    artifacts = emptyList(),
                )
                resolve(newChild, releaseMode, target, downloadArtifacts, true)
            }
    }

    private suspend fun fetchArtifactFromPom(dependency: DependencyConf.MavenDependency): File {
        val artifactName = "${dependency.artifactId}-${dependency.version}.jar"
        val artifactPath = (dependency.toPathParts() + artifactName).joinToString("/")
        return fetchMavenArtifact(artifactPath)
    }

    private suspend fun fetchMavenArtifact(artifactPath: String): File {
        val cacheFile = cacheRoot.nestedFile(artifactPath.replace("/", Path.DIRECTORY_SEPARATOR))
        if (cacheFile.exists()) {
            return cacheFile
        }
        val moduleUrl = "${mavenRepoUrl.trimEnd('/')}/$artifactPath"
        val response = http.get(moduleUrl)
        if (!response.status.isSuccess()) {
            // TODO: context.term.println("${failed("Failed")} Could not find module at $moduleUrl")
            error("Failed: Could not find module at $moduleUrl")
        }
        return cacheFile.apply {
            getParentFile()?.mkdirs()
            createNewFile()
            writeBytes(response.bodyAsChannel().toByteArray())
        }
    }

    private suspend fun fetchGradleModule(
        dependency: DependencyConf.MavenDependency,
        artifactRemotePath: String,
        artifactName: String = dependency.artifactId,
    ): GradleModule? {
        val artifactModuleName = "$artifactName-${dependency.version}.module"
        val artifactModuleCacheFile = cacheRoot
            .nestedFile(artifactRemotePath.replace("/", Path.DIRECTORY_SEPARATOR))
            .nestedFile(artifactModuleName)
        if (artifactModuleCacheFile.exists()) {
            return json.decodeFromString(artifactModuleCacheFile.readText())
        }
        val moduleUrl = "${mavenRepoUrl.trimEnd('/')}/$artifactRemotePath/$artifactModuleName"
        val response = http.get(moduleUrl)
        if (!response.status.isSuccess()) {
            return null
        }
        val bodyText = response.bodyAsText()
        artifactModuleCacheFile.apply {
            getParentFile()?.mkdirs()
            createNewFile()
            writeText(bodyText)
        }
        return json.decodeFromString(bodyText)
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

    private fun List<GradleModule.Variant>.findVariantFor(
        target: KotlinTarget,
    ): GradleModule.Variant? = if (target.isNative) {
        val knTarget = target.name.lowercase()
        firstOrNull { variant ->
            variant.attributes?.run {
                orgJetbrainsKotlinPlatformType == "native" && orgJetbrainsKotlinNativeTarget == knTarget
            } ?: false
        }
    } else if (target == KotlinTarget.JVM) {
        firstOrNull { variant ->
            variant.attributes?.run {
                orgJetbrainsKotlinPlatformType == "jvm" && orgGradleLibraryElements == "jar"
            } ?: false
        }
    } else {
        firstOrNull { variant ->
            variant.attributes?.run {
                orgJetbrainsKotlinJsCompiler == "ir" && orgJetbrainsKotlinPlatformType == "js"
            } ?: false
        }
    }

    private suspend fun downloadVariantArtifacts(
        target: KotlinTarget,
        variants: List<GradleModule.Variant>,
        downloadArtifacts: Boolean,
        dependency: DependencyConf.MavenDependency,
    ): Pair<GradleModule.Variant, List<String>> {
        val variant = checkNotNull(variants.findVariantFor(target)) {
            "Could not find variant for $target in ${dependency.toMavenString()}"
        }
        val availableAt = variant.availableAt
        return if (availableAt == null) {
            variant to if (downloadArtifacts) {
                variant.files.map { file ->
                    fetchArtifactFromMetadata(file, dependency, dependency.artifactId, dependency.version)
                        .getAbsolutePath()
                }
            } else {
                emptyList()
            }
        } else {
            followVariantRedirect(availableAt, dependency, downloadArtifacts)
        }
    }

    private suspend fun followVariantRedirect(
        availableAt: GradleModule.Variant.AvailableAt,
        dependency: DependencyConf.MavenDependency,
        downloadArtifacts: Boolean,
    ): Pair<GradleModule.Variant, List<String>> {
        val artifactRemotePath = dependency.groupId.split('.')
            .plus(availableAt.module)
            .plus(availableAt.version)
            .joinToString("/")
        val targetGradleModule = checkNotNull(fetchGradleModule(dependency, artifactRemotePath, availableAt.module)) {
            "Expected to find artifact target module"
        }
        val targetVariant = targetGradleModule.variants.first()
        val moduleName = availableAt.module
        val version = availableAt.version
        return targetVariant to if (downloadArtifacts) {
            targetVariant.files.map { file ->
                fetchArtifactFromMetadata(file, dependency, moduleName, version).getAbsolutePath()
            }
        } else {
            emptyList()
        }
    }
}
