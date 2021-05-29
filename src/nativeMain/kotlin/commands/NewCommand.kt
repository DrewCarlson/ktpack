package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import com.github.ajalt.mordant.terminal.*
import ktpack.util.*
import me.archinamon.fileio.*
import kotlin.system.*

private enum class Template { BIN, LIB }

class NewCommand(private val term: Terminal) : CliktCommand(
    help = "Create a new package.",
) {

    private val path by argument()

    private val name by option()
        .help("Set the resulting package name, defaults to the directory name")

    private val template by mutuallyExclusiveOptions(
        option("--bin", metavar = "")
            .help("Use a binary (application) template")
            .convert { Template.BIN },
        option("--lib", metavar = "")
            .help("Use a library template")
            .convert { Template.LIB },
    ).single().default(Template.BIN)

    override fun run() {
        val moduleName = name ?: path
        val targetDir = File(path)
        if (targetDir.exists()) {
            term.println("${failed("Failed")} path already exists for `$path`.")
            exitProcess(-1)
        }
        if (!targetDir.mkdirs()) {
            term.println("${failed("Failed")} path could not be generated for `$path`.")
            exitProcess(-1)
        }

        val manifest = File("${targetDir.getAbsolutePath()}/manifest.toml")
        if (manifest.createNewFile()) {
            manifest.writeText(newManifestSource(moduleName))
        } else {
            term.println("${failed("Failed")} manifest could not be generated for `${manifest.getAbsolutePath()}`.")
            exitProcess(-1)
        }

        val srcDir = File("${targetDir.getAbsolutePath()}/src")
        if (!srcDir.mkdirs()) {
            term.println("${failed("Failed")} source folder could not be created for `${srcDir.getAbsolutePath()}`.")
            exitProcess(-1)
        }

        when (template) {
            Template.BIN -> srcDir.generateSourceFile(term, "main.kt", NEW_BIN_SOURCE)
            Template.LIB -> srcDir.generateSourceFile(term, "lib.kt", NEW_LIB_SOURCE)
        }

        term.println(buildString {
            append(success("Created"))
            when (template) {
                Template.BIN -> append(" binary (application) ")
                Template.LIB -> append(" library ")
            }
            append("`$moduleName`")
            append(" package")
        })
    }
}

fun File.generateSourceFile(term: Terminal, fileName: String, contents: String) {
    val sourceFile = File("${getAbsolutePath()}/$fileName")
    if (sourceFile.createNewFile()) {
        sourceFile.writeText(contents)
    } else {
        term.println("${failed("Failed")} source could not be created at `${sourceFile.getAbsolutePath()}`.")
        exitProcess(-1)
    }
}

private fun newManifestSource(name: String) =
    """|[module]
       |name = "$name"
       |version = "0.1.0"
       |authors = "todo"
       |""".trimMargin()

private val NEW_BIN_SOURCE =
    """|fun main() {
       |    println("Hello, World!")
       |}
       |""".trimMargin()

private val NEW_LIB_SOURCE =
    """|fun sayHello() {
       |println("Hello, World!")
       |}
       |""".trimMargin()
