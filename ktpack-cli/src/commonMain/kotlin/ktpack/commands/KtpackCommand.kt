package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import ktpack.*
import ktpack.kotlin.KotlincInstalls
import ktpack.configuration.KtpackConf
import ktpack.jdk.JdkInstalls
import ktpack.task.TaskRunner
import ktpack.util.*
import okio.FileSystem
import okio.Path.Companion.toPath

class KtpackCommand(
    override val term: Terminal,
) : CliktCommand(
    help = "A simple tool for building and publishing Kotlin software.",
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
        pathFrom(KTPACK_ROOT, "config.json").run {
            if (!exists()) {
                FileSystem.SYSTEM.createDirectory(KTPACK_ROOT.toPath(), mustCreate = false)
                // check(File(KTPACK_ROOT).mkdirs()) {
                //    "Failed to create Ktpack folder $KTPACK_ROOT"
                // }
                println(toString())
                writeUtf8(json.encodeToString(KtpackUserConfig())) { error ->
                    logError(error)
                }
            }

            json.decodeFromString(readUtf8())
        }
    }

    override val jdkInstalls: JdkInstalls by lazy { JdkInstalls(this) }

    override val kotlinInstalls: KotlincInstalls by lazy { KotlincInstalls(this) }

    override val gitCli: GitCli = GitCli()

    override val rebuild: Boolean by option()
        .help("Force the pack script to be rebuilt, even if it has not changed.")
        .flag()

    override val stacktrace: Boolean by option()
        .help("Print the stacktrace in the case of an unhandled exception.")
        .flag()
    override val debug: Boolean by option().flag()

    override fun aliases(): Map<String, List<String>> = emptyMap()

    override val taskRunner: TaskRunner = TaskRunner()

    override suspend fun loadKtpackConf(filePath: String): KtpackConf {
        return ktpack.script.loadKtpackConf(this, filePath, rebuild)
    }

    override fun run() {
        Logger.mutableConfig.logWriterList = emptyList()
        Logger.addLogWriter(MordantLogWriter(term = term, debug = debug))
        currentContext.obj = this
        if (debug) {
            term.println("${info("Ktpack")} ${verbose(Ktpack.VERSION)}")
        }
    }
}
