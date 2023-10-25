package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import kotlinx.coroutines.runBlocking
import ktpack.CliContext
import ktpack.compilation.ModuleBuilder
import ktpack.util.workingDirectory
import okio.Path.Companion.toPath

class DependenciesCommand : CliktCommand(
    help = "Manage project dependencies.",
) {
    private val logger = Logger.withTag(DependenciesCommand::class.simpleName.orEmpty())
    private val context by requireObject<CliContext>()

    override fun run(): Unit = runBlocking {
        val packageConf = context.loadKtpackConf()
        val moduleBuilder = ModuleBuilder(packageConf.module, context, workingDirectory)

        val tree = moduleBuilder.resolveDependencyTree(packageConf.module, workingDirectory.toPath(), emptyList())
            .printDependencyTree()

        logger.i(tree)
    }
}
