package ktpack.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import kotlinx.coroutines.runBlocking
import ktfio.File
import ktpack.CliContext
import ktpack.compilation.ModuleBuilder
import ktpack.util.workingDirectory

class DependenciesCommand : CliktCommand(
    help = "Manage project dependencies.",
) {
    private val context by requireObject<CliContext>()

    override fun run(): Unit = runBlocking {
        val packageConf = context.loadKtpackConf()
        val moduleBuilder = ModuleBuilder(packageConf.module, context, workingDirectory)

        moduleBuilder.resolveDependencyTree(packageConf.module, File(workingDirectory), emptyList())
            .printDependencyTree()
    }
}
