package ktpack.manifest

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ktpack.configuration.DependencyScope

typealias DependenciesToml = Map<String, TargetDependenciesToml>

typealias TargetDependenciesToml = Map<String, DependencyToml>

@Serializable
sealed class DependencyToml {

    abstract val test: Boolean
    abstract val public: Boolean
    abstract val version: String?

    val scope: DependencyScope
        get() = if (test) {
            DependencyScope.TEST
        } else if (public) {
            DependencyScope.API
        } else {
            DependencyScope.IMPLEMENTATION
        }

    abstract val key: String

    fun withVersion(version: String): DependencyToml {
        return when (this) {
            is Git,
            is Local,
            -> this

            is Maven -> copy(version = version)
            is Npm -> copy(version = version)
        }
    }

    @Serializable
    data class Maven(
        val maven: String,
        override val version: String? = null,
        override val test: Boolean = false,
        override val public: Boolean = false,
    ) : DependencyToml() {

        @Transient
        override val key: String = maven

        val groupId: String
            get() = maven.substringBefore(":")

        val artifactId: String
            get() = maven.substringAfterLast(":")

        fun toMavenString(): String = "$maven:$version"
        fun toPathString(separator: String): String =
            toPathParts().joinToString(separator)

        fun toPathParts(): List<String> =
            maven.split(":").flatMap { it.split('.') } + version!!
    }

    @Serializable
    data class Local(
        val path: String,
        override val version: String? = null,
        override val test: Boolean = false,
        override val public: Boolean = false,
    ) : DependencyToml() {

        @Transient
        override val key: String = path
    }

    @Serializable
    data class Git(
        val git: String,
        val tag: String? = null,
        val branch: String? = null,
        val ref: String? = null,
        override val version: String? = null,
        override val test: Boolean = false,
        override val public: Boolean = false,
    ) : DependencyToml() {

        @Transient
        override val key: String = git
    }

    @Serializable
    data class Npm(
        val npm: String,
        val isDev: Boolean = false,
        override val version: String? = null,
        override val test: Boolean = false,
        override val public: Boolean = false,
    ) : DependencyToml() {

        @Transient
        override val key: String = npm
    }
}
