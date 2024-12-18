package ktpack.commands.nodejs

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.terminal.prompt
import kotlinx.io.files.Path
import ktpack.CliContext
import ktpack.util.*

class RemoveNodejsCommand : CliktCommand(name = "remove") {

    override fun help(context: Context): String {
        return context.theme.info("Remove an existing Nodejs version.")
    }

    private val logger = Logger.forClass<RemoveNodejsCommand>()
    private val context by requireObject<CliContext>()

    private val version by argument()
        .help("The Nodejs version to remove, can be a partial version string.")

    private val path by option()
        .help("The folder path where Nodejs versions are stored.")
        .convert { Path(it) }
        .defaultLazy { Path(checkNotNull(context.config.nodejs.rootPath)) }
        .validate { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs().exists()
        }

    override fun run() {
        // Find all Nodejs versions in the selected path
        val installs = context.nodejsInstalls.discover(path)
            .filter { it.version.drop(1).startsWith(version, ignoreCase = true) }
        if (installs.isEmpty()) {
            logger.i("${failed("Failed")} No existing Nodejs versions found in '$path'")
            return
        }

        // If the installation is used by the system, warn the user
        installs
            .onEach { install ->
                logger.i("Selected Nodejs ${info(install.version)} at ${install.path}")
            }
            .filter { it.isActive }
            .forEach { install ->
                logger.i("${warn("Warning")} This install is currently in your PATH variable: ${install.path}")
            }


        // Ask for user confirmation
        val response =
            context.term.prompt("${warn("Warning")} Confirm removal? [y/N]", default = "n", showDefault = false)
        if (!response.equals("y", true)) {
            context.term.println("Operation cancelled.")
            return
        }

        // Attempt to delete kotlinc folder
        installs.forEach { install ->
            if (Path(install.path).deleteRecursively()) {
                context.term.println("${success("Success")} Nodejs ${info(install.version)} was removed")
            } else {
                context.term.println("${failed("Failed")} Nodejs ${info(install.version)} was not removed")
            }
        }
    }
}
