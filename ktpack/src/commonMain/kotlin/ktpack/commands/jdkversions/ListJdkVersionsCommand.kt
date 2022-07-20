package ktpack.commands.jdkversions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.table.table
import kotlinx.coroutines.*
import ktfio.File
import ktfio.filePathSeparator
import ktpack.KtpackContext
import ktpack.util.USER_HOME
import ktpack.util.info
import ktpack.util.success
import ktpack.util.title

class ListJdkVersionsCommand : CliktCommand(
    name = "list",
    help = "List installed Jdk versions.",
) {

    private val context by requireObject<KtpackContext>()

    private val path by option(
        help = "The folder path where Jdk installs are located."
    ).convert { File(it) }
        .default(File("${USER_HOME}$filePathSeparator.jdks"))
        .validate { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs()
        }

    override fun run(): Unit = runBlocking {
        val installations = JdkInstalls.discover(path)
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