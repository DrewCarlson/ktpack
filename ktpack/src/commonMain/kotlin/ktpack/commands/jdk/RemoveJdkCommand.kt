package ktpack.commands.jdk

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import ktfio.File
import ktfio.deleteRecursively
import ktpack.CliContext
import ktpack.util.*

class RemoveJdkCommand : CliktCommand(
    name = "remove",
    help = "Remove an existing JDK version.",
) {

    private val version by argument()
        .help("The JDK version to remove, can be a partial version string.")

    private val distribution by option("--distribution", "-d")
        .help("The JDK distribution to remove.")
        .enum<JdkDistribution>()

    private val path by option()
        .help("The folder path where JDKs are stored.")
        .convert { File(it) }
        .defaultLazy { File(checkNotNull(context.config.jdk.rootPath)) }
        .validate { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs()
        }

    private val context by requireObject<CliContext>()

    override fun run() {
        // Find all JDKs in the selected path
        val installs = context.jdkInstalls.discover(path)
        if (installs.isEmpty()) {
            context.term.println("${failed("Failed")} No existing JDKs found in '$path'")
            return
        }

        // Apply user filters
        val matches = if (distribution == null) {
            installs.filter { it.version.startsWith(version) }
        } else {
            installs.filter { it.version.startsWith(version) && it.distribution == distribution }
        }

        // Nothing to remove if empty
        if (matches.isEmpty()) {
            context.term.println("${failed("Failed")} No JDKs installed matching filters")
            return
        }

        // If one match is found, use it
        // If more than one match is found, let user choose
        // If user does not select a version, quit
        val match = matches.singleOrNull() ?: userSelectInstall(matches) ?: return

        context.term.println("Selected JDK ${info(match.distribution.name)} ${info(match.version)} at ${match.path}")

        // If the installation is used by the system or intellij, warn the user
        if (match.isActive || match.isIntellijInstall) {
            val message = buildString {
                append("${warn("Warning")} This install is currently ")
                if (match.isActive) append("in your PATH variable ")
                if (match.isActive && match.isIntellijInstall) append("and ")
                if (match.isIntellijInstall) append("managed by Intellij IDEA")
            }
            context.term.println(message)
        }

        // Ask for user confirmation
        val response = context.term.prompt("${warn("Warning")} Confirm removal? [y/N]", default = "n", showDefault = false)
        if (!response.equals("y", true)) {
            context.term.println("Operation cancelled.")
            return
        }

        // Attempt to delete jdk folder
        if (File(match.path).deleteRecursively()) {
            context.term.println("${success("Success")} JDK ${info(match.distribution.name)} ${info(match.version)} was removed")
        } else {
            context.term.println("${failed("Failed")} JDK install was not removed")
        }
    }

    private fun userSelectInstall(matches: List<InstallationDetails>): InstallationDetails? {
        context.term.println("Found ${info(matches.size.toString())} similar JDK installs:")
        context.term.printJdkSelectionTable(matches)
        context.term.println()
        val index = context.term.prompt("Enter install id or cancel with any other value")?.toIntOrNull()
        return if (index == null) {
            context.term.println("Response was empty or not a number, done.")
            null
        } else if (index <= matches.lastIndex) {
            matches[index] // Use selection
        } else {
            context.term.println("Response was not between 0..${matches.size}, done.")
            null
        }
    }

    private fun Terminal.printJdkSelectionTable(installs: List<InstallationDetails>) = println(
        table {
            cellBorders = Borders.NONE
            padding {
                vertical = 0
                horizontal = 1
            }
            header { row { cells("id", "Distribution", "Version") } }
            body {
                installs.forEachIndexed { index, install ->
                    row {
                        cell(verbose(index.toString()))
                        cell(install.distribution)
                        cell(install.version)
                    }
                }
            }
        },
    )
}
