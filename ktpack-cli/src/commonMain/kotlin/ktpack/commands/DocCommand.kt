package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.mordant.rendering.TextColors.brightWhite
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.reset
import kotlinx.coroutines.runBlocking
import ktpack.CliContext
import ktpack.compilation.tools.models.DokkaConfiguration
import ktpack.compilation.tools.models.SourceSet
import ktpack.compilation.tools.models.SourceSetID
import ktpack.configuration.ModuleConf
import ktpack.util.*
import okio.Path.Companion.toPath

class DocCommand : CliktCommand(
    name = "doc",
    help = "Build project documentation.",
) {
    private val context by requireObject<CliContext>()
    private val logger = Logger.withTag(DocCommand::class.simpleName.orEmpty())

    override fun run(): Unit = runBlocking {
        val moduleConf = context.loadKtpackConf().module
        val jdk = checkNotNull(context.jdkInstalls.getDefaultJdk())
        if (context.dokka.getDefaultCli() == null) {
            if (!context.dokka.download("1.9.10")) {
                logger.i("${failed("Failed")} Dokka is missing and failed to download")
                return@runBlocking
            }
        }
        val docOutputDir = (workingDirectory / "out" / "docs").toString()

        val configuration = DokkaConfiguration(
            moduleName = moduleConf.name,
            moduleVersion = moduleConf.version,
            outputDir = docOutputDir,
            sourceSets = createSourceSets(moduleConf)
        )
        logger.i("${info("Doc")} Building docs into $docOutputDir")
        val (_, duration) = measureSeconds {
            context.term.loadingIndeterminate(
                animate = { text, duration ->
                    bold(brightWhite(text)) + reset(white(" ${duration.inWholeSeconds}s"))
                },
            ) {
                context.dokka.runDokka(
                    javaPath = jdk.path.toPath() / "bin" / "java",
                    outPath = workingDirectory / "out",
                    dokkaConfiguration = configuration,
                )
            }
        }
        logger.i("${success("Doc")} Completed build successfully in ${duration}s")
    }

    private fun createSourceSets(moduleConf: ModuleConf): List<SourceSet> {
        return listOf(
            SourceSet(
                displayName = "common",
                analysisPlatform = "common",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "common",
                ),
                sourceRoots = listOf((workingDirectory / "src" / "common" / "kotlin").toString())
            ),
            SourceSet(
                displayName = "native",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "native",
                ),
                sourceRoots = listOf((workingDirectory / "src" / "native" / "kotlin").toString()),
            ),
            SourceSet(
                displayName = "posix",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "posix",
                ),
                sourceRoots = listOf((workingDirectory / "src" / "posix" / "kotlin").toString()),
            ),
            SourceSet(
                displayName = "linux",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "linux",
                ),
                sourceRoots = listOf((workingDirectory / "src" / "linux" / "kotlin").toString()),
            ),
            SourceSet(
                displayName = "macos",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "macos",
                ),
                sourceRoots = listOf(
                    (workingDirectory / "src" / "macos" / "kotlin").toString(),
                    (workingDirectory / "src" / "macosarm64" / "kotlin").toString(),
                    (workingDirectory / "src" / "macosx64" / "kotlin").toString(),
                ),
            ),
            SourceSet(
                displayName = "mingw",
                analysisPlatform = "native",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "mingw",
                ),
                sourceRoots = listOf((workingDirectory / "src" / "mingw" / "kotlin").toString()),
            ),
            SourceSet(
                displayName = "jvm",
                analysisPlatform = "jvm",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "jvm",
                ),
                sourceRoots = listOf((workingDirectory / "src" / "jvm" / "kotlin").toString()),
            ),
            SourceSet(
                displayName = "js",
                analysisPlatform = "js",
                sourceSetID = SourceSetID(
                    scopeId = moduleConf.name,
                    sourceSetName = "js",
                ),
                sourceRoots = listOf(
                    (workingDirectory / "src" / "js" / "kotlin").toString(),
                    (workingDirectory / "src" / "jsbrowser" / "kotlin").toString(),
                    (workingDirectory / "src" / "jsnode" / "kotlin").toString(),
                ),
            ),
        )
    }
}
