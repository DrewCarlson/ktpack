package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import ktpack.CliContext
import ktpack.compilation.ModuleBuilder
import ktpack.compilation.dependencies.models.dependencyTreeString
import ktpack.compilation.dependencies.models.resolveAndFlatten
import ktpack.configuration.KotlinTarget
import ktpack.util.workingDirectory
import okio.Path.Companion.DIRECTORY_SEPARATOR

class DependenciesCommand : CliktCommand(
    help = "Manage project dependencies.",
) {
    private val logger = Logger.withTag(DependenciesCommand::class.simpleName.orEmpty())
    private val context by requireObject<CliContext>()
    private val target by option("--target", "-t")
        .help("The target platform to list dependencies for.")
        .enum<KotlinTarget>()
    private val fetch by option("--fetch", "-f")
        .help("Resolve and fetch all artifacts for the dependency tree.")
        .flag(default = false)
    private val resolve by option("--resolve", "-r")
        .help("Resolve the final dependency list for the package.")
        .flag(default = false)

    override fun run(): Unit = runBlocking {
        val packageConf = context.loadKtpackConf()
        val moduleBuilder = ModuleBuilder(packageConf.module, context, workingDirectory)

        val tree = moduleBuilder.resolveRootDependencyTree(listOfNotNull(target))

        if (fetch) {
            val processedTree = moduleBuilder.fetchArtifacts(
                tree.resolveAndFlatten(),
                releaseMode = false,
                // TODO: Fix default target selection
                target = target ?: KotlinTarget.JVM
            )
            logger.i(processedTree.dependencyTreeString())
            logger.i("The following artifacts were collected:")
            processedTree
                .flatMap { it.artifacts }
                .forEach { path ->
                    logger.i(" - ${path.substringAfterLast(DIRECTORY_SEPARATOR)}")
                }
        } else if (resolve) {
            logger.i(tree.resolveAndFlatten().dependencyTreeString())
        } else {
            logger.i(tree.dependencyTreeString())
        }
    }
}
