package ktpack

import com.appmattus.crypto.Algorithm
import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import ktfio.File
import ktfio.readBytes
import ktfio.readText
import ktfio.writeText
import ktpack.commands.*
import ktpack.commands.jdk.*
import ktpack.commands.kotlin.*
import ktpack.configuration.*
import ktpack.util.TEMP_DIR
import ktpack.util.failed
import ktpack.util.measureSeconds
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import kotlin.system.*

const val MANIFEST_NAME = "package.main.kts"

@SharedImmutable
val json = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    useAlternativeNames = false
}

@OptIn(ExperimentalXmlUtilApi::class)
@SharedImmutable
val xml = XML {
    unknownChildHandler = UnknownChildHandler { _, _, _, _, _ -> emptyList() }
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

suspend fun loadManifest(context: CliContext, path: String): ManifestConf {
    val digest = Algorithm.MD5.createDigest().apply { update(File(path).readBytes()) }.digest().toHexString()
    val cacheKey = File(TEMP_DIR, ".ktpack-manifest-cache-$digest")
    val (module, duration) = measureSeconds {
        if (cacheKey.exists()) {
            //println("Reading manifest from cache")
            json.decodeFromString(cacheKey.readText())
        } else {
            //println("Processing manifest")
            // TODO: Log in debug only
            Dispatchers.Default { executePackage(context, path) }.also { manifestConf ->
                //println(cacheKey.getAbsolutePath())
                if (cacheKey.createNewFile()) {
                    //println("Caching new manifest output")
                    cacheKey.writeText(json.encodeToString(manifestConf))
                }
            }
        }
    }
    println("Manifest loaded in ${duration}s: $path")
    return module
}

private fun ByteArray.toHexString(): String {
    return joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }
}
