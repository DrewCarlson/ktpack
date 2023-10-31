package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import ktpack.CliContext
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
import ktpack.util.*
import okio.Path
import okio.Path.Companion.toPath

class CleanCommand : CliktCommand(
    help = "Remove generated artifacts and folders.",
) {

    private val userTarget by option("--target", "-t")
        .help("The target platform to clean.")
        .enum<KotlinTarget>()

    private val context by requireObject<CliContext>()

    override fun run() = runBlocking {
        val userTarget = userTarget
        val packageConf = context.loadKtpackConf()
        val module = packageConf.module
        val moduleBuilder = ModuleBuilder(module, context, workingDirectory)

        val dependencyTree = moduleBuilder.resolveRootDependencyTree(listOfNotNull(userTarget))
        dependencyTree
            .mapNotNull { child -> child.localModule?.name }
            .map { name ->
                // TODO: Handle cleaning local child dependency build folder
                if (userTarget == null) {
                    pathFrom(name, "out")
                } else {
                    pathFrom(name, "out", userTarget.name.lowercase())
                }
            }

        val outDir = if (userTarget == null) {
            "out".toPath()
        } else {
            pathFrom("out", userTarget.name.lowercase())
        }
        tryDeleteDirectory(outDir, userTarget)
    }

    private fun tryDeleteDirectory(outDir: Path, target: KotlinTarget?) {
        if (!outDir.exists() || !outDir.isDirectory()) {
            context.term.println("${success("Clean")} No files to delete")
        } else if (outDir.deleteRecursively()) {
            context.term.println("${success("Clean")} Build files for ${verbose(target?.name ?: "all targets")} have been removed")
        } else {
            context.term.println("${failed("Clean")} Unable to delete build files")
        }
    }
}
