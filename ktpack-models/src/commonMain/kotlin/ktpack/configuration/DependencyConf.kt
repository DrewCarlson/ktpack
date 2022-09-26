package ktpack.configuration

import kotlinx.serialization.Serializable

@Serializable
sealed class DependencyConf {

    abstract val scope: DependencyScope

    @Serializable
    data class LocalPathDependency(
        val path: String,
        override val scope: DependencyScope
    ) : DependencyConf()

    @Serializable
    data class GitDependency(
        val gitUrl: String,
        val tag: String?,
        val branch: String?,
        val version: String?,
        override val scope: DependencyScope
    ) : DependencyConf()

    @Serializable
    data class MavenDependency(
        val groupId: String,
        val artifactId: String,
        val version: String,
        override val scope: DependencyScope
    ) : DependencyConf() {
        fun toMavenString(): String = "$groupId:$artifactId:$version"
    }

    @Serializable
    data class NpmDependency(
        val name: String,
        val version: String,
        val isDev: Boolean,
        override val scope: DependencyScope
    ) : DependencyConf()
}
