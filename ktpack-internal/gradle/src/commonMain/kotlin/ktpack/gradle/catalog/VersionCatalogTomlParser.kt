package ktpack.gradle.catalog

import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import ktpack.toml

internal object VersionCatalogTomlParser {

    fun parse(tomlString: String): VersionCatalogToml {
        val libsToml = toml.tomlParser.parseString(tomlString)

        val versionsToml = libsToml.getChildTable("versions")
        val librariesToml = libsToml.getChildTable("libraries")
        val versions = versionsToml.mapValues { (_, value) -> parseVersionNode(value) }
        val libraries = librariesToml.mapValues { (key, value) ->
            val fields = value.children
            val moduleToml = fields.firstOrNull { it.name == "module" } as? TomlKeyValuePrimitive
            val groupToml = fields.firstOrNull { it.name == "group" } as? TomlKeyValuePrimitive
            val nameToml = fields.firstOrNull { it.name == "name" } as? TomlKeyValuePrimitive
            val versionToml = checkNotNull(fields.firstOrNull { it.name == "version" }) {
                "No version specified for library $key"
            }
            val version = parseLibraryVersion(versionToml, versions)
            when {
                moduleToml != null -> VersionCatalogToml.Library(
                    module = moduleToml.value.content.toString(),
                    version = version,
                )

                groupToml != null && nameToml != null ->
                    VersionCatalogToml.Library(
                        module = "${groupToml.value.content}:${nameToml.value.content}",
                        version = version,
                    )

                else -> error("Failed to parse library $key")
            }
        }

        return VersionCatalogToml(versions = versions, libraries = libraries)
    }

    private fun parseLibraryVersion(
        versionToml: TomlNode,
        versions: Map<String, VersionCatalogToml.Version>,
    ): VersionCatalogToml.Version {
        val refToml = (versionToml as? TomlTable)
            ?.children
            ?.firstOrNull { it.name == "ref" } as? TomlKeyValuePrimitive
        return if (refToml == null) {
            parseVersionNode(versionToml)
        } else {
            val ref = refToml.value.content.toString()
            checkNotNull(versions[ref]) {
                "No version found with name $ref for library"
            }
        }
    }

    private fun parseVersionNode(value: TomlNode) = when (value) {
        is TomlKeyValuePrimitive -> {
            VersionCatalogToml.Version(value.value.content.toString())
        }

        is TomlTable -> {
            val strictlyToml = value.children
                .firstOrNull { it.name == "strictly" } as? TomlKeyValuePrimitive
            val version = checkNotNull(strictlyToml).value.content.toString()
            VersionCatalogToml.Version(version, strict = true)
        }

        else -> {
            error("Failed to parse version: ${value::class}")
        }
    }

    private fun TomlFile.getChildTable(name: String): Map<String, TomlNode> {
        return children
            .firstOrNull { it.name == name }
            ?.children
            ?.associateBy { it.name }
            .orEmpty()
    }
}
