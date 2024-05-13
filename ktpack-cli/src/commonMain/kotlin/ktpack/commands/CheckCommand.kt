package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.*
import kotlinx.coroutines.runBlocking
import ktpack.*

class CheckCommand : CliktCommand(
    help = "Check a package for errors.",
) {

    private val filePath by argument("file")
        .help("The package file to validate.")
        .default(MANIFEST_FILENAME)

    private val context by requireObject<CliContext>()

    override fun run() = runBlocking {
        val packageConf = context.loadManifestToml(filePath)
        // TODO: Validate all package properties
        println(packageConf)
    }
}
