package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.*
import kotlinx.coroutines.runBlocking
import ktpack.*
import ktpack.manifest.MANIFEST_FILENAME

class CheckCommand : CliktCommand() {

    override fun help(context: Context): String {
        return context.theme.info("Check a package for errors.")
    }

    private val filePath by argument("file")
        .help("The package file to validate.")
        .default(MANIFEST_FILENAME)

    private val context by requireObject<CliContext>()

    override fun run() = runBlocking {
        val packageConf = context.load(filePath)
        // TODO: Validate all package properties
        println(packageConf)
    }
}
