package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import ktfio.File
import ktfio.readText
import ktfio.writeText
import ktpack.*
import ktpack.commands.jdk.JdkInstalls
import ktpack.configuration.PackageConf
import ktpack.task.TaskRunner
import ktpack.util.KTPACK_ROOT
import ktpack.util.info
import ktpack.util.verbose

class KtpackCommand(
    override val term: Terminal,
) : CliktCommand(
    help = "A simple tool for building and publishing Kotlin software."
),
    CliContext {

    override val http: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    override val config: KtpackUserConfig by lazy {
        File(KTPACK_ROOT, "config.json").run {
            if (!exists()) {
                check(File(KTPACK_ROOT).mkdirs()) {
                    "Failed to create Ktpack folder $KTPACK_ROOT"
                }
                println(getAbsolutePath())
                if (createNewFile()) {
                    writeText(json.encodeToString(KtpackUserConfig()))
                } else {
                    error("Failed to write: ${getAbsolutePath()}")
                }
            }

            json.decodeFromString(readText())
        }
    }

    override val jdkInstalls: JdkInstalls by lazy { JdkInstalls(this) }

    override val stacktrace: Boolean by option()
        .help("Print the stacktrace in the case of an unhandled exception.")
        .flag()
    override val debug: Boolean by option().flag()

    override fun aliases(): Map<String, List<String>> = emptyMap()

    override val taskRunner: TaskRunner = TaskRunner()

    override suspend fun loadPackage(filePath: String, forceRebuild: Boolean): PackageConf {
        return ktpack.script.loadPackageConf(this, filePath, forceRebuild)
    }

    override fun run() {
        currentContext.obj = this
        if (debug) {
            term.println("${info("Ktpack")} ${verbose(Ktpack.VERSION)}")
        }
    }
}
