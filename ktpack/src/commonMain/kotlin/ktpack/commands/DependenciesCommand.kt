package ktpack.commands

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
    private val context by requireObject<CliContext>()

    override fun run(): Unit = runBlocking {
        val packageConf = context.loadKtpackConf()
        val moduleBuilder = ModuleBuilder(packageConf.module, context, workingDirectory)

        moduleBuilder.resolveDependencyTree(packageConf.module, workingDirectory.toPath(), emptyList())
            .printDependencyTree()
    }
}
