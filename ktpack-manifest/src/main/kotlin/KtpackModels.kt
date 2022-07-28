package ktpack.configuration

import kotlinx.serialization.Serializable
import Target


@Serializable
class KtpackModule(
    var name: String,
    var version: String = "",
    val authors: MutableList<String> = mutableListOf(),
    var description: String? = null,
    val keywords: MutableList<String> = mutableListOf(),
    var readme: String? = null,
    var homepage: String? = null,
    var repository: String? = null,
    var license: String? = null,
    var publish: Boolean = false,
    var autobin: Boolean = true,
    val targets: MutableList<Target> = mutableListOf(),
    var kotlinVersion: String? = null,
    val dependencies: MutableList<DependencyContainer> = mutableListOf(),
) {

    fun dependencies(vararg target: Target, configure: KtpackDependencyBuilder.() -> Unit) {
        dependencies.add(KtpackDependencyBuilder(target.toList()).apply(configure).build())
    }

    fun dependenciesJs(vararg target: Target, configure: KtpackDependencyJsBuilder.() -> Unit) {
        val targetList = if (target.isEmpty()) listOf(Target.JS_NODE, Target.JS_BROWSER) else target.toList()
        dependencies.add(KtpackDependencyJsBuilder(targetList).apply(configure).build())
    }
}

@Serializable
class DependencyContainer(
    val targets: List<Target>,
    val dependencies: List<KtpackDependency>,
)

@Serializable
enum class DependencyScope {
    IMPLEMENTATION,
    API,
    TEST,
    COMPILE,
}

@Serializable
sealed class KtpackDependency {

    abstract val scope: DependencyScope

    @Serializable
    data class LocalPathDependency(
        val path: String,
        val version: String?,
        override val scope: DependencyScope,
    ) : KtpackDependency()

    @Serializable
    data class GitDependency(
        val gitUrl: String,
        val tag: String?,
        val branch: String?,
        val version: String?,
        override val scope: DependencyScope,
    ) : KtpackDependency()

    @Serializable
    data class MavenDependency(
        val groupId: String,
        val artifactId: String,
        val version: String?,
        override val scope: DependencyScope,
    ) : KtpackDependency()

    @Serializable
    data class NpmDependency(
        val name: String,
        val version: String,
        val isDev: Boolean,
        override val scope: DependencyScope,
    ) : KtpackDependency()
}