package ktpack.commands.jdk

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.table.grid
import kotlinx.coroutines.*
import ktfio.File
import ktpack.CliContext
import ktpack.util.info

class ListJdkCommand : CliktCommand(
    name = "list",
    help = "List installed JDK versions.",
) {

    private val context by requireObject<CliContext>()

    private val path by option()
        .help("The folder path where JDK installs are located.")
        .convert { File(it) }
        .defaultLazy { File(checkNotNull(context.config.jdkRootPath)) }
        .validate { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs()
        }

    override fun run(): Unit = runBlocking {
        val installations = context.jdkInstalls.discover(path)
        context.term.println("Found ${info(installations.size.toString())} JDK installation(s)")
        context.term.println()
        context.term.println(
            grid {
                installations.forEach { install ->
                    row {
                        cell("[ENV]".takeIf { install.isActive }.orEmpty())
                        cell(install.distribution)
                        cell(install.version)
                        cell("(IntelliJ)".takeIf { install.isIntellijInstall }.orEmpty())
                        cell(install.path) {
                            overflowWrap = OverflowWrap.ELLIPSES
                        }
                    }
                }
            }
        )
    }
}
