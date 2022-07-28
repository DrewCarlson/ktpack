package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import ktfio.File
import ktfio.deleteRecursively
import ktpack.CliContext
import ktpack.MANIFEST_NAME
import ktpack.configuration.Target
import ktpack.loadManifest
import ktpack.util.ModuleBuilder
import ktpack.util.failed
import ktpack.util.success
import ktpack.util.verbose
import platform.posix.getcwd
import platform.windows.MAX_PATH

class CleanCommand : CliktCommand(
    help = "Remove generated artifacts and folders.",
) {

    private val userTarget by option("--target", "-t")
        .help("The target platform to clean.")
        .enum<Target>()

    private val context by requireObject<CliContext>()

    override fun run() = runBlocking {
        val userTarget = userTarget
        val launchPath = memScoped {
            allocArray<ByteVar>(MAX_PATH).apply { getcwd(this, MAX_PATH) }.toKString()
        }
        val manifest = loadManifest(context, MANIFEST_NAME)
        val module = manifest.module
        val moduleBuilder = ModuleBuilder(module, context, launchPath)

        val dependencyTree = moduleBuilder.resolveDependencyTree(module, File(launchPath), listOfNotNull(userTarget))
        dependencyTree.children.mapNotNull { child ->
            child.localModule?.name
        }.forEach { name ->
            if (userTarget == null) {
                File(name, "out", )
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

    private fun tryDeleteDirectory(outDir: File, target: Target?) {
        if (!outDir.exists() || !outDir.isDirectory()) {
            context.term.println("${success("Clean")} No files to delete")
        } else if (outDir.deleteRecursively()) {
            context.term.println("${success("Clean")} Build files for ${verbose(target?.name ?: "all targets")} have been removed")
        } else {
            context.term.println("${failed("Clean")} Unable to delete build files")
        }
    }
}
