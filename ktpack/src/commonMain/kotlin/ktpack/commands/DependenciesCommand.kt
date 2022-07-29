package ktpack.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import kotlinx.coroutines.runBlocking
import ktfio.File
import ktpack.CliContext
import ktpack.MANIFEST_NAME
import ktpack.loadManifest
import ktpack.util.ModuleBuilder
import ktpack.util.workingDirectory

class DependenciesCommand : CliktCommand(
    help = "Manage project dependencies.",
) {
    private val context by requireObject<CliContext>()

    override fun run(): Unit = runBlocking {
        val manifest = loadManifest(context, MANIFEST_NAME)
        val moduleBuilder = ModuleBuilder(manifest.module, context, workingDirectory)

        moduleBuilder.resolveDependencyTree(manifest.module, File(workingDirectory), emptyList())
            .printDependencyTree()
    }
}
