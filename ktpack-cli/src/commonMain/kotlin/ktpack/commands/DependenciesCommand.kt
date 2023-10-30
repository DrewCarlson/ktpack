package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import ktpack.CliContext
import ktpack.compilation.ModuleBuilder
import ktpack.compilation.dependencies.models.printDependencyTree
import ktpack.configuration.KotlinTarget
import ktpack.util.workingDirectory
import okio.Path.Companion.toPath

class DependenciesCommand : CliktCommand(
    help = "Manage project dependencies.",
) {
    private val logger = Logger.withTag(DependenciesCommand::class.simpleName.orEmpty())
    private val context by requireObject<CliContext>()
    private val target by option("--target", "-t")
        .help("The target platform to list dependencies for.")
        .enum<KotlinTarget>()

    override fun run(): Unit = runBlocking {
        val packageConf = context.loadKtpackConf()
        val moduleBuilder = ModuleBuilder(packageConf.module, context, workingDirectory)

        val tree = moduleBuilder.resolveDependencyTree(packageConf.module, workingDirectory, listOfNotNull(target))
            .printDependencyTree()

        logger.i(tree)
    }
}
