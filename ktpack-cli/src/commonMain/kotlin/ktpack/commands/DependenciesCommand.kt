package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.SystemPathSeparator
import ktpack.CliContext
import ktpack.compilation.ModuleBuilder
import ktpack.compilation.dependencies.models.dependencyTreeString
import ktpack.compilation.dependencies.models.resolveAndFlatten
import ktpack.configuration.DependencyScope
import ktpack.configuration.KotlinTarget
import ktpack.util.PlatformUtils
import ktpack.util.workingDirectory

class DependenciesCommand : CliktCommand() {

    override fun help(context: Context): String {
        return context.theme.info("Manage project dependencies.")
    }

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
    private val scopes by option("--scope", "-s")
        .help("Limit the results to the selected scope(s), defaults to all scopes.")
        .enum<DependencyScope>()
        .multiple(DependencyScope.entries)

    override fun run(): Unit = runBlocking {
        val packageConf = context.loadManifestToml()
        val moduleBuilder = ModuleBuilder(packageConf, context, workingDirectory)

        val tree = moduleBuilder.resolveRootDependencyTree(listOfNotNull(target), scopes)

        if (fetch) {
            val processedTree = moduleBuilder.fetchArtifacts(
                tree.resolveAndFlatten(),
                releaseMode = false,
                target = target ?: PlatformUtils.getHostTarget()
            )
            logger.i(processedTree.dependencyTreeString())
            logger.i("The following artifacts were collected:")
            processedTree
                .flatMap { it.artifacts }
                .forEach { path ->
                    logger.i(" - ${path.substringAfterLast(SystemPathSeparator)}")
                }
        } else if (resolve) {
            logger.i(tree.resolveAndFlatten().dependencyTreeString())
        } else {
            logger.i(tree.dependencyTreeString())
        }
    }
}
