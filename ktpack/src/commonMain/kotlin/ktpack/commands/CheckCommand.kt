package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.options.help
import kotlinx.coroutines.runBlocking
import ktpack.*

class CheckCommand : CliktCommand(
    help = "Check a package for errors."
) {

    private val filePath by argument("file")
        .help("The manifest file to validate.")
        .default(MANIFEST_NAME)

    private val context by requireObject<CliContext>()

    override fun run() = runBlocking {
        val manifest = context.loadManifest(filePath)
        // TODO: Validate all manifest properties
        println(manifest)
    }
}
