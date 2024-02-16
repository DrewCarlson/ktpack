package ktpack.gradle.catalog

import org.junit.Test
import kotlin.test.assertEquals

class LibsGeneratorTest {

    @Test
    fun test() {
        val catalog = VersionCatalogToml(
            libraries = mapOf(
                "kotlin-stdlib" to VersionCatalogToml.Library(
                    module = "org.jetbrains.kotlin:kotlin-stdlib",
                    version = VersionCatalogToml.Version("1.9.22"),
                ),
                "kotlin-test" to VersionCatalogToml.Library(
                    module = "org.jetbrains.kotlin:kotlin-test",
                    version = VersionCatalogToml.Version("1.9.22"),
                ),
            ),
            versions = mapOf(
                "kotlin" to VersionCatalogToml.Version("1.9.22", strict = false),
            ),
        )

        assertEquals(
            """
                import kotlin.String

                public class GeneratedLibKotlin {
                  public val stdlib: String = "org.jetbrains.kotlin:kotlin-stdlib:1.9.22"

                  public val test: String = "org.jetbrains.kotlin:kotlin-test:1.9.22"
                }

                public class GeneratedVersions {
                  public val kotlin: String = "1.9.22"
                }

                public class GeneratedCatalog {
                  public val versions: GeneratedVersions = GeneratedVersions()

                  public val kotlin: GeneratedLibKotlin = GeneratedLibKotlin()
                }

                public val libs: GeneratedCatalog = GeneratedCatalog()
            
            """.trimIndent(),
            LibsGenerator.generate(catalog).toString(),
        )
    }

    @Test
    fun test2() {
        val catalog = VersionCatalogToml(
            libraries = mapOf(
                "a" to VersionCatalogToml.Library(
                    module = "a",
                    version = VersionCatalogToml.Version("1"),
                ),
                "a-b" to VersionCatalogToml.Library(
                    module = "b",
                    version = VersionCatalogToml.Version("2"),
                ),
                "a-b-c" to VersionCatalogToml.Library(
                    module = "c",
                    version = VersionCatalogToml.Version("3"),
                ),
                "a-b-d" to VersionCatalogToml.Library(
                    module = "d",
                    version = VersionCatalogToml.Version("4"),
                ),
            ),
            versions = emptyMap(),
        )

        assertEquals(
            """
                import kotlin.String
                import ktpack.configuration.DependencyConfProvider

                public class GeneratedLibA : DependencyConfProvider {
                  public val b: GeneratedLibB = GeneratedLibB()

                  public fun getDependencyConf(): String = "a:1"
                }

                public class GeneratedLibB : DependencyConfProvider {
                  public val c: String = "c:3"

                  public val d: String = "d:4"

                  public fun getDependencyConf(): String = "b:2"
                }

                public class GeneratedVersions

                public class GeneratedCatalog {
                  public val versions: GeneratedVersions = GeneratedVersions()

                  public val a: GeneratedLibA = GeneratedLibA()
                }

                public val libs: GeneratedCatalog = GeneratedCatalog()

            """.trimIndent(),
            LibsGenerator.generate(catalog).toString(),
        )
    }
}
