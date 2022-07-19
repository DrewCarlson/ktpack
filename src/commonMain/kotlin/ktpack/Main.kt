package ktpack

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.cinterop.*
import ktfio.*
import ktpack.commands.*
import ktpack.commands.ktversions.*
import ktpack.configuration.*
import ktpack.util.failed
import kotlin.system.*

const val MANIFEST_NAME = "manifest.toml"

@SharedImmutable
private val json = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    useAlternativeNames = false
}

data class KtpackOptions(
    val stacktrace: Boolean,
    val debug: Boolean,
)

fun main(args: Array<String>) {
    val term = Terminal()
    val http = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }
    val command = KtpackCommand(term).apply {
        subcommands(
            CheckCommand(term),
            BuildCommand(term),
            RunCommand(term),
            TestCommand(term),
            NewCommand(term),
            InitCommand(term),
            CleanCommand(term),
            VersionCommand(term),
            DependenciesCommand(term),
            KotlinVersionsCommand(term)
                .subcommands(ListKotlinVersionsCommand(term, http)),
        )
    }

    try {
        command.main(args)
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
        exitProcess(-1)
    }
}

fun loadManifest(manifestFile: String): ManifestConf = memScoped {
    val manifestContent = try {
        File(manifestFile).readText()
    } catch (e: FileNotFoundException) {
        println("Failed to find '$manifestFile'.")
        exitProcess(-1)
    } catch (e: IllegalFileAccess) {
        println("Failed to read '$manifestFile', check file permissions.")
        exitProcess(-1)
    }

    return ManifestConf.fromToml(manifestContent)
}
