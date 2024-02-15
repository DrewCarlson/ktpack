package ktpack.gradle.catalog

import kotlinx.serialization.Serializable


@Serializable
data class VersionCatalogToml(
    val versions: Map<String, Version>,
    val libraries: Map<String, Library>,
) {

    companion object {
        fun parse(tomlString: String): VersionCatalogToml {
            return VersionCatalogTomlParser.parse(tomlString)
        }
    }

    /*
    TODO: Expand support for libs.versions.toml
    require: the required version
    strictly: the strict version
    prefer: the preferred version
    reject: the list of rejected versions
    rejectAll: a boolean to reject all versions
     */
    @Serializable
    data class Version(
        val value: String,
        val strict: Boolean = false,
    )

    @Serializable
    data class Library(
        val module: String,
        val version: Version,
    )
}
