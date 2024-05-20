package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import ktpack.CliContext
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.DependencyScope
import ktpack.configuration.KotlinTarget
import ktpack.util.*

class CleanCommand : CliktCommand() {

    override fun help(context: Context): String {
        return context.theme.info("Remove generated artifacts and folders.")
    }

    private val userTarget by option("--target", "-t")
        .help("The target platform to clean.")
        .enum<KotlinTarget>()

    private val context by requireObject<CliContext>()

    override fun run() = runBlocking {
        val userTarget = userTarget
        val manifestToml = context.loadManifestToml()
        val moduleBuilder = ModuleBuilder(manifestToml, context, workingDirectory)

        val dependencyTree = moduleBuilder.resolveRootDependencyTree(
            KotlinTarget.entries,
            DependencyScope.entries,
            includeCommon = true
        )
        dependencyTree
            .mapNotNull { child -> child.localManifest?.module?.name }
            .map { name ->
                // TODO: Handle cleaning local child dependency build folder
                if (userTarget == null) {
                    Path(name, "out")
                } else {
                    Path(name, "out", userTarget.name.lowercase())
                }
            }

        val outDir = if (userTarget == null) {
            Path("out")
        } else {
            Path("out", userTarget.name.lowercase())
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
