package ktpack

import com.github.ajalt.clikt.core.*
import kotlinx.cinterop.*
import ktpack.commands.*
import ktpack.configuration.*
import kotlin.system.*

const val MANIFEST_NAME = "manifest.toml"

fun main(args: Array<String>) = KtpackCommand()
    .subcommands(
        CheckCommand(),
        BuildCommand(),
        RunCommand(),
        TestCommand(),
        NewCommand(),
        InitCommand(),
        CleanCommand(),
        VersionCommand(),
    )
    .main(args)

fun loadManifest(manifestFile: String): ManifestConf = memScoped {
    val manifestContent = try {
        readFile(manifestFile)
    } catch (e: IllegalArgumentException) {
        println("Failed to find or open '$manifestFile'.")
        exitProcess(-1)
    }

    return ManifestConf.fromToml(manifestContent)
}

