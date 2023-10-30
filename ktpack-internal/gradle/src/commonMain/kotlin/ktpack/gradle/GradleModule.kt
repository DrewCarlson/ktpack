package ktpack.gradle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models representing the Gradle Module Metadata specification.
 *
 * https://github.com/gradle/gradle/blob/fd135460bb8587bd85c6d507c0bfd805d0d3e73b/subprojects/docs/src/docs/design/gradle-module-metadata-latest-specification.md
 */
@Serializable
data class GradleModule(
    val formatVersion: String,
    val component: Component? = null,
    val createdBy: CreatedBy? = null,
    val variants: List<Variant> = listOf(),
) {
    @Serializable
    data class Component(
        val group: String,
        val module: String,
        val version: String,
        val url: String? = null,
    )

    @Serializable
    data class CreatedBy(
        val gradle: Gradle? = null,
    ) {
        @Serializable
        data class Gradle(
            val version: String,
            val buildId: String? = null,
        )
    }

    @Serializable
    data class Variant(
        val name: String,
        val attributes: Attributes? = null,
        @SerialName("available-at")
        val availableAt: AvailableAt? = null,
        val dependencies: List<Dependency> = listOf(),
        //val dependencyConstraints: DependencyConstraints
        val files: List<File> = listOf(),
        val capabilities: List<Capability> = emptyList(),
    ) {

        @Serializable
        data class Capability(
            val group: String,
            val name: String,
            val version: String? = null,
        )

        @Serializable
        data class AvailableAt(
            val group: String,
            val module: String,
            val url: String,
            val version: String,
        )

        @Serializable
        data class Dependency(
            val group: String,
            val module: String,
            val version: Version,
            val excludes: List<Excludes>? = null,
            val reason: String? = null,
            val attributes: Attributes? = null,
            val requestedCapabilities: List<Capability>? = null,
            val endorseStrictVersions: Boolean = false,
            // val thirdPartyCompatibility
        ) {
            @Serializable
            data class Version(
                val requires: String,
                val prefers: String? = null,
                val strictly: String? = null,
                val rejects: String? = null,
            )

            @Serializable
            data class Excludes(
                val group: String,
                val module: String,
            )
        }

        @Serializable
        data class File(
            val md5: String,
            val name: String,
            val sha1: String,
            val sha256: String,
            val sha512: String,
            val size: Int,
            val url: String,
        )
    }

    @Serializable
    data class Attributes(
        val artifactType: String? = null,
        @SerialName("org.gradle.status")
        val orgGradleStatus: String? = null,
        @SerialName("org.gradle.category")
        val orgGradleCategory: String? = null,
        @SerialName("org.gradle.libraryelements")
        val orgGradleLibraryElements: String? = null,
        @SerialName("org.gradle.usage")
        val orgGradleUsage: String? = null,
        @SerialName("org.jetbrains.kotlin.js.compiler")
        val orgJetbrainsKotlinJsCompiler: String? = null,
        @SerialName("org.jetbrains.kotlin.native.target")
        val orgJetbrainsKotlinNativeTarget: String? = null,
        @SerialName("org.jetbrains.kotlin.platform.type")
        val orgJetbrainsKotlinPlatformType: String? = null,
    )
}
