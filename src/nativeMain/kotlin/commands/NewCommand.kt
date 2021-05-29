package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.terminal.*
import com.github.ajalt.mordant.rendering.TextColors.*
import me.archinamon.fileio.*

class NewCommand(private val term: Terminal) : CliktCommand(
    help = "Create a new package.",
) {

    private val path by argument()
    private val template by option()
        .switch("--bin" to true, "--lib" to false)
        .default(true)
    private val name by option()

    override fun run() {
        val targetDir = File(path)
        if (targetDir.exists()) {
            error("File already exists at ${targetDir.getAbsolutePath()}")
        } else {
            targetDir.mkdirs()
            val manifest = File("${targetDir.getAbsolutePath()}/manifest.toml")
            manifest.createNewFile()
            manifest.writeText("""
                [module]
                name = "${name ?: path}"
                version = "0.0.1"
                authors = "todo"
            """.trimIndent())

            File("${targetDir.getAbsolutePath()}/src").mkdirs()

            val binFile = File("${targetDir.getAbsolutePath()}/main.kt")
            binFile.writeText("""
                fun main() {
                    println("Hello, World!")
                }
            """.trimIndent())

            term.println("${brightGreen("Created")} binary (application) `${name ?: path}` package")
        }
    }
}
