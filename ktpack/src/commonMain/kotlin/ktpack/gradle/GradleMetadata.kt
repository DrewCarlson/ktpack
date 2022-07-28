package ktpack.gradle

import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName

// https://github.com/gradle/gradle/blob/master/subprojects/docs/src/docs/design/gradle-module-metadata-latest-specification.md
@Serializable
data class GradleModule(
    val component: Component? = null,
    val createdBy: CreatedBy? = null,
    val formatVersion: String,
    val variants: List<Variant> = listOf()
) {
    @Serializable
    data class Component(
        val attributes: Attributes? = null,
        val group: String,
        val module: String,
        val version: String,
        val url: String? = null,
    )

    @Serializable
    data class CreatedBy(
        val gradle: Gradle? = null
    ) {
        @Serializable
        data class Gradle(
            val version: String,
            val buildId: String? = null,
        )
    }

    @Serializable
    data class Variant(
        val attributes: Attributes? = null,
        @SerialName("available-at")
        val availableAt: AvailableAt? = null,
        val dependencies: List<Dependency> = listOf(),
        val files: List<File> = listOf(),
        val name: String
    ) {

        @Serializable
        data class AvailableAt(
            val group: String,
            val module: String,
            val url: String,
            val version: String
        )

        @Serializable
        data class Dependency(
            val group: String,
            val module: String,
            val version: Version
        ) {
            @Serializable
            data class Version(
                val requires: String
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
            val url: String
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
        val orgGradleLibraryelements: String? = null,
        @SerialName("org.gradle.usage")
        val orgGradleUsage: String? = null,
        @SerialName("org.jetbrains.kotlin.js.compiler")
        val orgJetbrainsKotlinJsCompiler: String? = null,
        @SerialName("org.jetbrains.kotlin.native.target")
        val orgJetbrainsKotlinNativeTarget: String? = null,
        @SerialName("org.jetbrains.kotlin.platform.type")
        val orgJetbrainsKotlinPlatformType: String? = null
    )
}