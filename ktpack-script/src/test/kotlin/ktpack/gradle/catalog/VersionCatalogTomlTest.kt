package ktpack.gradle.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class VersionCatalogTomlTest {

    @Test
    fun test_ParseVersions_Simple() {
        val tomlString = """
            [versions]
            test = "1.0.0"
        """
        val versionCatalog = VersionCatalogTomlParser.parse(tomlString)

        val expected = VersionCatalogToml(
            libraries = emptyMap(),
            versions = mapOf(
                "test" to VersionCatalogToml.Version("1.0.0", strict = false),
            ),
        )

        assertEquals(expected, versionCatalog)
    }


    @Test
    fun test_ParseVersions_Complex() {
        val tomlString = """
                [versions]
                kotlin = { strictly = "1.9.22" }
            """
        val versionCatalog = VersionCatalogTomlParser.parse(tomlString)
        val expected = VersionCatalogToml(
            libraries = emptyMap(),
            versions = mapOf(
                "kotlin" to VersionCatalogToml.Version("1.9.22", strict = true),
            ),
        )

        assertEquals(expected, versionCatalog)
    }

    @Test
    fun test_Library_VersionRef() {
        val tomlString = """
            [versions]
            test = "1.0.0"
            
            [libraries]
            test = { group = "org.test", name = "test", version.ref = "test" }
        """
        val versionCatalog = VersionCatalogTomlParser.parse(tomlString)
        val expected = VersionCatalogToml(
            versions = mapOf(
                "test" to VersionCatalogToml.Version("1.0.0", strict = false),
            ),
            libraries = mapOf(
                "test" to VersionCatalogToml.Library(
                    module = "org.test:test",
                    version = VersionCatalogToml.Version("1.0.0", strict = false),
                ),
            ),
        )

        assertEquals(expected, versionCatalog)
    }

    @Test
    fun test_Library_VersionString() {
        val tomlString = """
            [libraries]
            test = { group = "org.test", name = "test", version = "1.0.0" }
        """
        val versionCatalog = VersionCatalogTomlParser.parse(tomlString)
        val expected = VersionCatalogToml(
            versions = emptyMap(),
            libraries = mapOf(
                "test" to VersionCatalogToml.Library(
                    module = "org.test:test",
                    version = VersionCatalogToml.Version("1.0.0", strict = false),
                ),
            ),
        )

        assertEquals(expected, versionCatalog)
    }
}

