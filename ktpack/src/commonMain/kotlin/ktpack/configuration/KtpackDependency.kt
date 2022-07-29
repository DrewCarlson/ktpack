package ktpack.configuration

import kotlinx.serialization.Serializable

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
        val version: String,
        override val scope: DependencyScope,
    ) : KtpackDependency() {
        fun toMavenString(): String = "$groupId:$artifactId:$version"
    }

    @Serializable
    data class NpmDependency(
        val name: String,
        val version: String,
        val isDev: Boolean,
        override val scope: DependencyScope,
    ) : KtpackDependency()
}
