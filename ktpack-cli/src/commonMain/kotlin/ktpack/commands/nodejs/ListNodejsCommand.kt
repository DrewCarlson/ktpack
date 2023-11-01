package ktpack.commands.nodejs

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.table.grid
import kotlinx.coroutines.*
import ktpack.CliContext
import ktpack.util.exists
import ktpack.util.info
import ktpack.util.isDirectory
import ktpack.util.mkdirs
import okio.Path.Companion.toPath

class ListNodejsCommand : CliktCommand(
    name = "list",
    help = "List installed Nodejs versions.",
) {

    private val context by requireObject<CliContext>()

    private val path by option()
        .help("The folder path where Nodejs versions installs are located.")
        .convert { it.toPath() }
        .defaultLazy { checkNotNull(context.config.nodejs.rootPath).toPath() }
        .validate { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs().exists()
        }

    override fun run(): Unit = runBlocking {
        val installations = context.nodejsInstalls.discover(path)
        context.term.println("Found ${info(installations.size.toString())} Nodejs installation(s)")
        context.term.println()
        context.term.println(
            grid {
                installations.forEach { install ->
                    row {
                        cell("[ENV]".takeIf { install.isActive }.orEmpty())
                        cell(install.version)
                        cell(install.path) {
                            overflowWrap = OverflowWrap.ELLIPSES
                        }
                    }
                }
            },
        )
    }
}
