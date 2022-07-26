package ktpack

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import ktfio.*
import ktpack.commands.*
import ktpack.commands.jdk.*
import ktpack.commands.kotlin.*
import ktpack.configuration.*
import ktpack.util.failed
import kotlin.system.*

const val MANIFEST_NAME = "manifest.toml"

@SharedImmutable
val json = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    useAlternativeNames = false
}

fun main(args: Array<String>) {
    val term = Terminal()
    val command = KtpackCommand(term).apply {
        subcommands(
            CheckCommand(),
            BuildCommand(),
            RunCommand(),
            TestCommand(),
            NewCommand(),
            InitCommand(),
            CleanCommand(),
            DocCommand(),
            VersionCommand(),
            DependenciesCommand(),
            KotlinCommand()
                .subcommands(InstallKotlinCommand())
                .subcommands(RemoveKotlinCommand())
                .subcommands(FindKotlinCommand()),
            JdkCommand()
                .subcommands(InstallJdkCommand())
                .subcommands(RemoveJdkCommand())
                .subcommands(FindJdkCommand())
                .subcommands(ListJdkCommand()),
        )
    }

    try {
        command.main(args)

        runBlocking { command.taskRunner.execute() }
    } catch (e: Throwable) {
        term.println(
            buildString {
                append(failed("Failed"))
                append(" Uncaught error: ")
                e.message?.let(::append)
            }
        )
        if (command.stacktrace) {
            term.println(e.stackTraceToString())
        }
        exitProcess(1)
    }
}

fun loadManifest(path: String): ManifestConf = memScoped {
    val file = File(path)
    val manifestContent = try {
        if (file.exists()) file.readText() else null
    } catch (e: FileNotFoundException) {
        println("Failed to find '$path'.")
        exitProcess(1)
    } catch (e: IllegalFileAccess) {
        println("Failed to read '$path', check file permissions.")
        exitProcess(1)
    } ?: run {
        println("No $MANIFEST_NAME in ${file.getAbsolutePath().substringBeforeLast(filePathSeparator)}")
        exitProcess(1)
    }

    return ManifestConf.fromToml(manifestContent)
}
