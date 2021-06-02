package ktpack

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.cinterop.*
import ktpack.commands.*
import ktpack.commands.ktversions.*
import ktpack.configuration.*
import me.archinamon.fileio.*
import kotlin.system.*

const val MANIFEST_NAME = "manifest.toml"

@ThreadLocal
private val json = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    useAlternativeNames = false
}

fun main(args: Array<String>) {
    val term = Terminal()
    val http = HttpClient {
        Json {
            serializer = KotlinxSerializer(json)
        }
    }
    KtpackCommand(term)
        .subcommands(
            CheckCommand(term),
            BuildCommand(term),
            RunCommand(term),
            TestCommand(term),
            NewCommand(term),
            InitCommand(term),
            CleanCommand(term),
            VersionCommand(term),
            KotlinVersionsCommand(term)
                .subcommands(ListKotlinVersionsCommand(term, http)),
        )
        .main(args)
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

