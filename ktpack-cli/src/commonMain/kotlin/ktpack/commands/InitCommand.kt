package ktpack.commands

import com.github.ajalt.clikt.core.*
import ktpack.CliContext
import ktpack.PACK_SCRIPT_FILENAME
import ktpack.util.exists
import ktpack.util.failed
import ktpack.util.pathFrom
import ktpack.util.workingDirectory

class InitCommand : CliktCommand(
    help = "Create a new package in an existing directory.",
) {
    private val context by requireObject<CliContext>()

    override fun run() {
        val packFile = pathFrom(workingDirectory, PACK_SCRIPT_FILENAME)
        if (packFile.exists()) {
            context.term.println("${failed("Failed")} A pack file already exists in this directory.")
            return
        }
        TODO("Not yet implemented")
    }
}
