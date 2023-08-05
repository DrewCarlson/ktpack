package ktpack.commands.kotlin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.table.grid
import kotlinx.coroutines.runBlocking
import ktfio.File
import ktpack.CliContext
import ktpack.util.info

class ListKotlinCommand : CliktCommand(
    name = "list",
    help = "List installed Kotlin versions.",
) {

    private val context by requireObject<CliContext>()

    private val path by option()
        .help("The folder path where Kotlin installs are located.")
        .convert { File(it) }
        .defaultLazy { File(checkNotNull(context.config.kotlin.rootPath)) }
        .validate { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs()
        }

    override fun run() = runBlocking {
        val installs = context.kotlinInstalls.discover(path)
        context.term.println("Found ${info(installs.size.toString())} Kotlin installation(s)")
        context.term.println()
        context.term.println(
            grid {
                installs.forEach { install ->
                    row {
                        cell("[ENV]".takeIf { install.isActive }.orEmpty())
                        cell(install.type)
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
