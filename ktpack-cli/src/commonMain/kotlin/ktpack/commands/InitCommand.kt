package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import ktpack.CliContext
import ktpack.MANIFEST_FILENAME
import ktpack.util.exists
import ktpack.util.failed
import ktpack.util.workingDirectory

class InitCommand : CliktCommand(
    help = "Create a new package in an existing directory.",
) {
    private val context by requireObject<CliContext>()
    private val logger = Logger.withTag(InitCommand::class.simpleName.orEmpty())

    override fun run() {
        val packFile = workingDirectory / MANIFEST_FILENAME
        if (packFile.exists()) {
            logger.i("${failed("Failed")} A pack file already exists in this directory.")
            return
        }

        logger.i("${failed("Failed")} init command is not currently implemented, use `new` instead.")
    }
}
