package ktpack.compilation.tools

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.invoke
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.encodeToString
import ksubprocess.exec
import ktpack.compilation.dependencies.MavenDependencyResolver
import ktpack.compilation.dependencies.models.DependencyNode
import ktpack.compilation.dependencies.models.resolveAndFlatten
import ktpack.compilation.tools.models.DokkaConfiguration
import ktpack.compilation.tools.models.PluginsConfiguration
import ktpack.configuration.KotlinTarget
import ktpack.json
import ktpack.manifest.DependencyToml
import ktpack.util.*

// https://kotlinlang.org/docs/dokka-cli.html
// https://kotlin.github.io/dokka/1.7.10/user_guide/cli/usage/
class DokkaCli(
    private val mavenResolver: MavenDependencyResolver,
) {

    private val logger = Logger.forClass<DokkaCli>()

    /**
     * Run Dokka with the provided configuration.
     *
     * **NOTE:** The dokka output is stored in [DokkaConfiguration.outputDir] and
     * not the [outPath] which is used only for build related files.
     *
     * @param javaPath An absolute path to the `java` binary to launch dokka with.
     * @param outPath An absolute path to the directory will build files are stored.
     * @param dokkaConfiguration The dokka configuration to launch dokka with.
     */
    suspend fun runDokka(
        javaPath: Path,
        outPath: Path,
        dokkaConfiguration: DokkaConfiguration,
        dokkaVersion: String,
    ) {
        require(javaPath.isAbsolute) { "Dokka javaPath must be an absolute directory $javaPath" }
        require(outPath.isAbsolute) { "Dokka outPath must be an absolute directory $outPath" }

        val dokkaConfigPath = Path(outPath, "dokka", "config-${dokkaConfiguration.moduleName}.json")
        val pluginArtifacts = downloadDokkaPlugins(dokkaVersion)
        val cli = pluginArtifacts.first()

        val updatedDokkaConfig = dokkaConfiguration.copy(
            pluginsClasspath = pluginArtifacts.drop(1),
            pluginsConfiguration = listOf(
                PluginsConfiguration(
                    fqPluginName = "org.jetbrains.dokka.base.DokkaBase",
                    serializationFormat = "JSON",
                    values = "{}",
                ),
            ),
        )
        logger.d { "Writing dokka config to ${dokkaConfigPath}\n$updatedDokkaConfig" }
        SystemFileSystem.createDirectories(dokkaConfigPath.parent!!)
        dokkaConfigPath.writeString(json.encodeToString(updatedDokkaConfig))
        try {
            val result = Dispatchers.IO {
                exec {
                    arg(javaPath.toString())
                    arg("-jar")
                    arg(cli)
                    arg(dokkaConfigPath.toString())
                    logger.d { "Launching dokka cli:\n${arguments.joinToString(" ")}" }
                }
            }
            logger.d { result.output }
            logger.d { result.errors }
        } finally {
            dokkaConfigPath.delete()
        }
    }

    suspend fun downloadDokkaPlugins(dokkaVersion: String): List<String> {
        val dependencies = listOf(
            resolveDokkaPlugin("org.jetbrains.dokka:dokka-cli", dokkaVersion),
            resolveDokkaPlugin("org.jetbrains.dokka:dokka-base", dokkaVersion),
            resolveDokkaPlugin("org.jetbrains.dokka:analysis-kotlin-descriptors", dokkaVersion),
        ).resolveAndFlatten()
        val resolvedDependencies = mavenResolver.resolveArtifacts(
            nodes = dependencies,
            releaseMode = false,
            target = KotlinTarget.JVM,
        )
        return resolvedDependencies
            .mapNotNull { dependency ->
                val conf = dependency.dependencyConf as DependencyToml.Maven
                if (packedDependencies.contains(conf.maven)) {
                    null // Dependency is packed in dokka-cli, don't include
                } else {
                    dependency.artifacts.single()
                }
            }
    }

    private suspend fun resolveDokkaPlugin(coordinates: String, version: String): DependencyNode {
        return mavenResolver.resolve(
            node = DependencyNode(
                localManifest = null,
                dependencyConf = DependencyToml.Maven(
                    maven = coordinates,
                    version = version,
                ),
                children = emptyList(),
                artifacts = emptyList(),
            ),
            releaseMode = false,
            target = KotlinTarget.JVM,
        )
    }

    // dokka-cli is a shadow jar with all the dependencies included,
    // this list contains all the direct dependencies for dokka-base
    // and analysis-kotlin-descriptors.
    // TODO: Using custom plugins potentially fails if they include
    //   dependencies that are packed in dokka-cli.
    //   Will need to come up with a better way to deal with this,
    //   Possibly publishing a version of dokka-cli as a normal jar.
    private val packedDependencies = listOf(
        "org.jsoup:jsoup",
        "com.google.code.findbugs:jsr305",
        "org.jetbrains:markdown",
        "org.jetbrains.dokka:analysis-markdown",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core",
        "org.jetbrains:annotations",
        "com.fasterxml.jackson.module:jackson-module-kotlin",
        "com.fasterxml.jackson.core:jackson-databind",
        "com.fasterxml.jackson.core:jackson-core",
        "com.fasterxml.jackson.core:jackson-annotations",
    )
}
