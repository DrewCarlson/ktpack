package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import ktfio.File
import ktfio.deleteRecursively
import ktpack.CliContext
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
import ktpack.util.*

class CleanCommand : CliktCommand(
    help = "Remove generated artifacts and folders.",
) {

    private val userTarget by option("--target", "-t")
        .help("The target platform to clean.")
        .enum<KotlinTarget>()

    private val context by requireObject<CliContext>()

    override fun run() = runBlocking {
        val userTarget = userTarget
        val packageConf = context.loadPackage()
        val module = packageConf.module
        val moduleBuilder = ModuleBuilder(module, context, workingDirectory)

        val dependencyTree = moduleBuilder.resolveDependencyTree(module, File(workingDirectory), listOfNotNull(userTarget))
        dependencyTree.children.mapNotNull { child ->
            child.localModule?.name
        }.forEach { name ->
            if (userTarget == null) {
                File(name, "out",)
            } else {
                File(name, "out", userTarget.name.lowercase())
            }
        }
        val outDir = if (userTarget == null) {
            File("out")
        } else {
            File("out", userTarget.name.lowercase())
        }
        tryDeleteDirectory(outDir, userTarget)
    }

    private fun tryDeleteDirectory(outDir: File, target: KotlinTarget?) {
        if (!outDir.exists() || !outDir.isDirectory()) {
            context.term.println("${success("Clean")} No files to delete")
        } else if (outDir.deleteRecursively()) {
            context.term.println("${success("Clean")} Build files for ${verbose(target?.name ?: "all targets")} have been removed")
        } else {
            context.term.println("${failed("Clean")} Unable to delete build files")
        }
    }
}
