package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import ktfio.File
import ktfio.deleteRecursively
import ktfio.filePathSeparator
import ktpack.KtpackContext
import ktpack.configuration.Target
import ktpack.util.failed
import ktpack.util.success
import ktpack.util.verbose

class CleanCommand : CliktCommand(
    help = "Remove generated artifacts and folders.",
) {

    private val targetPlatform by option("--target")
        .help("The target platform to clean.")
        .enum<Target>()

    private val context by requireObject<KtpackContext>()

    override fun run() {
        val targetPlatform = targetPlatform
        val outDir = if (targetPlatform == null) {
            File("out")
        } else {
            File("out${filePathSeparator}$targetPlatform")
        }
        tryDeleteDirectory(outDir, targetPlatform)
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
