package ktpack.configuration

import ktpack.gradle.catalog.LibsGenerator
import ktpack.gradle.catalog.VersionCatalogTomlParser
import java.nio.file.Files
import kotlin.io.path.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm


@KotlinScript(
    displayName = "Ktpack Script",
    fileExtension = "kts",
    filePathPattern = ".*pack\\.kts",
    compilationConfiguration = KtpackScriptCompilationConfiguration::class,
)
public open class KtpackScriptTemplate(
    @Suppress("unused")
    public val args: Array<String>, // DO NOT REMOVE
) : KtpackScriptScope()

internal class KtpackScriptCompilationConfiguration : ScriptCompilationConfiguration(
    {
        ide { acceptedLocations(ScriptAcceptedLocation.Project) }
        baseClass(KtpackScriptTemplate::class)
        defaultImports(
            "ktpack.configuration.*",
            "ktpack.configuration.KotlinTarget",
            "ktpack.configuration.KotlinTarget.*",
        )
        jvm {
            dependenciesFromClassContext(
                KtpackScriptTemplate::class,
                wholeClasspath = true,
                //libraries = arrayOf("kotlin-stdlib", "kotlin-reflect")
            )
        }

        // TODO: At this point we don't have any info about the script path,
        //   assume the pack script is in the current working directory.
        //   This is true at least for instances launched by ktpack-cli.
        val libsTomlPath = Path("libs.versions.toml")
        if (libsTomlPath.exists()) {
            val generatedLibsFile = Files.createTempFile("libs-generated-", ".kt")
            val libsToml = VersionCatalogTomlParser.parse(libsTomlPath.readText())
            val code = LibsGenerator.generate(libsToml).toString()
            generatedLibsFile.writeText(code)

            importScripts.append(FileScriptSource(generatedLibsFile.toFile()))
        }
    },
)
