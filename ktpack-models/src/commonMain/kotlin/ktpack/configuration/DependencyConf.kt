package ktpack.configuration

import kotlinx.serialization.Serializable

@Serializable
sealed class DependencyConf {

    abstract val scope: DependencyScope

    abstract val key: String

    abstract val version: String?

    @Serializable
    data class LocalPathDependency(
        val path: String,
        override val scope: DependencyScope
    ) : DependencyConf() {
        override val key: String = path

        override val version: String? = null
    }

    @Serializable
    data class GitDependency(
        val gitUrl: String,
        val tag: String?,
        val branch: String?,
        override val version: String?,
        override val scope: DependencyScope
    ) : DependencyConf() {
        override val key: String = gitUrl
    }

    @Serializable
    data class MavenDependency(
        val groupId: String,
        val artifactId: String,
        override val version: String,
        override val scope: DependencyScope
    ) : DependencyConf() {
        fun toMavenString(): String = "$groupId:$artifactId:$version"
        fun toPathString(separator: String): String =
            groupId.split('.')
                .plus(artifactId)
                .plus(version)
                .joinToString(separator)

        fun toPathParts(): List<String> =
            groupId.split('.') + artifactId + version

        override val key: String = "$groupId:$artifactId"
    }

    @Serializable
    data class NpmDependency(
        val name: String,
        override val version: String,
        val isDev: Boolean,
        override val scope: DependencyScope
    ) : DependencyConf() {
        override val key: String = name
    }
}
