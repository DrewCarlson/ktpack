package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import kotlinx.io.files.Path
import ktpack.CliContext
import ktpack.manifest.MANIFEST_FILENAME
import ktpack.util.exists
import ktpack.util.failed
import ktpack.util.forClass
import ktpack.util.workingDirectory

class InitCommand : CliktCommand() {

    override fun help(context: Context): String {
        return context.theme.info("Create a new package in an existing directory.")
    }

    private val context by requireObject<CliContext>()
    private val logger = Logger.forClass<InitCommand>()

    override fun run() {
        val packFile = Path(workingDirectory, MANIFEST_FILENAME)
        if (packFile.exists()) {
            logger.i("${failed("Failed")} A pack file already exists in this directory.")
            return
        }

        logger.i("${failed("Failed")} init command is not currently implemented, use `new` instead.")
    }
}
