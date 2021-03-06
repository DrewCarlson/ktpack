package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.mordant.terminal.*
import ktpack.*

class CheckCommand(
    private val term: Terminal
) : CliktCommand(
    help = "Check a package for errors."
) {

    private val filePath by argument("file", help = "The manifest file to validate.")
        .default(MANIFEST_NAME)

    override fun run() {
        val manifest = loadManifest(filePath)
        // TODO: Validate all manifest properties
        println(manifest)
    }
}
