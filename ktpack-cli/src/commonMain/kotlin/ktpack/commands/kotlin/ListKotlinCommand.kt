package ktpack.commands.kotlin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.table.grid
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import ktpack.CliContext
import ktpack.util.exists
import ktpack.util.info
import ktpack.util.isDirectory
import ktpack.util.mkdirs

class ListKotlinCommand : CliktCommand(name = "list") {

    override fun help(context: Context): String {
        return context.theme.info("List installed Kotlin versions.")
    }

    private val context by requireObject<CliContext>()

    private val path by option()
        .help("The folder path where Kotlin installs are located.")
        .convert { Path(it) }
        .defaultLazy { Path(checkNotNull(context.config.kotlin.rootPath)) }
        .validate { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs().exists()
        }

    override fun run() = runBlocking {
        val installs = context.kotlinInstalls.discover(path)
        context.term.println("Found ${info(installs.size.toString())} Kotlin installation(s) in '${context.config.kotlin.rootPath}'")
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
