package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import ktpack.CliContext
import ktpack.compilation.tools.models.DokkaConfiguration
import ktpack.compilation.tools.models.SourceSet
import ktpack.compilation.tools.models.SourceSetID
import ktpack.manifest.ModuleToml
import ktpack.util.*
import webserver.runWebServer

class DocCommand : CliktCommand(name = "doc") {

    override fun help(context: Context): String {
        return context.theme.info("Build project documentation.")
    }

    private val context by requireObject<CliContext>()
    private val logger = Logger.withTag(DocCommand::class.simpleName.orEmpty())

    private val serve by option("--serve", "-s")
        .help("After building docs, start a web server to view them.")
        .flag()

    private val httpPort by option("--port", "-p")
        .help("The port for the web server with the --serve flag.")
        .int()
        .default(9543)

    override fun run(): Unit = runBlocking {
        val manifest = context.loadManifestToml()
        val jdk = checkNotNull(context.jdkInstalls.getDefaultJdk())
        val dokkaVersion = manifest.docs.version ?: context.config.dokkaVersion
        if (context.dokka.getCli(dokkaVersion) == null) {
            if (!context.dokka.download(dokkaVersion)) {
                logger.i("${failed("Failed")} Dokka is missing and failed to download")
                return@runBlocking
            }
        }
        val docOutputDir = Path(workingDirectory, "out", "docs").toString()

        val configuration = DokkaConfiguration(
            moduleName = manifest.module.name,
            moduleVersion = manifest.module.version,
            outputDir = docOutputDir,
            sourceSets = createSourceSets(manifest.module),
        )
        logger.i("${info("Doc")} Building docs into $docOutputDir")
        val (_, duration) = measureSeconds {
            context.term.loadingIndeterminate {
                context.dokka.runDokka(
                    javaPath = Path(jdk.path, "bin", "java"),
                    outPath = Path(workingDirectory, "out"),
                    dokkaConfiguration = configuration,
                    dokkaVersion = dokkaVersion
                )
            }
        }
        logger.i("${success("Doc")} Completed build successfully in ${duration}s")

        if (serve) {
            runWebServer(
                httpPort,
                onServerStarted = {
                    logger.i("${info("HTTP Server")} Available at http://localhost:$httpPort")
                },
            ) {
                route("{...}") { respondDirectory(docOutputDir) }
            }
        }
    }

    private fun createSourceSets(moduleConf: ModuleToml): List<SourceSet> {
        return listOf(
            SourceSet(
                displayName = "common",
                analysisPlatform = "common",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "common",
                ),
                sourceRoots = listOf(Path(workingDirectory, "src", "common", "kotlin").toString()),
            ),
            SourceSet(
                displayName = "native",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "native",
                ),
                sourceRoots = listOf(Path(workingDirectory, "src", "native", "kotlin").toString()),
            ),
            SourceSet(
                displayName = "posix",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "posix",
                ),
                sourceRoots = listOf(Path(workingDirectory, "src", "posix", "kotlin").toString()),
            ),
            SourceSet(
                displayName = "linux",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "linux",
                ),
                sourceRoots = listOf(Path(workingDirectory, "src", "linux", "kotlin").toString()),
            ),
            SourceSet(
                displayName = "macos",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "macos",
                ),
                sourceRoots = listOf(
                    Path(workingDirectory, "src", "macos", "kotlin").toString(),
                    Path(workingDirectory, "src", "macosarm64", "kotlin").toString(),
                    Path(workingDirectory, "src", "macosx64", "kotlin").toString(),
                ),
            ),
            SourceSet(
                displayName = "mingw",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "mingw",
                ),
                sourceRoots = listOf(Path(workingDirectory, "src", "mingw", "kotlin").toString()),
            ),
            SourceSet(
                displayName = "jvm",
                analysisPlatform = "jvm",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "jvm",
                ),
                sourceRoots = listOf(Path(workingDirectory, "src", "jvm", "kotlin").toString()),
            ),
            SourceSet(
                displayName = "js",
                analysisPlatform = "js",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "js",
                ),
                sourceRoots = listOf(
                    Path(workingDirectory, "src", "js", "kotlin").toString(),
                    Path(workingDirectory, "src", "jsbrowser", "kotlin").toString(),
                    Path(workingDirectory, "src", "jsnode", "kotlin").toString(),
                ),
            ),
        )
    }
}
