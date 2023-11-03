package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import ktpack.*
import ktpack.compilation.tools.DokkaCli
import ktpack.toolchain.kotlin.KotlincInstalls
import ktpack.configuration.KtpackConf
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.nodejs.NodejsInstalls
import ktpack.util.*
import okio.Path.Companion.toPath

class KtpackCommand(
    override val term: Terminal,
) : CliktCommand(
    help = "A simple tool for building and publishing Kotlin software.",
),
    CliContext {

    override val config: KtpackUserConfig by lazy {
        (KTPACK_ROOT / "config.json").run {
            if (!exists()) {
                SystemFs.createDirectory(KTPACK_ROOT, mustCreate = false)
                // check(File(KTPACK_ROOT).mkdirs()) {
                //    "Failed to create Ktpack folder $KTPACK_ROOT"
                // }
                writeUtf8(json.encodeToString(KtpackUserConfig())) { error ->
                    logError(error)
                }
            }

            json.decodeFromString(readUtf8())
        }
    }

    override val jdkInstalls: JdkInstalls by lazy { JdkInstalls(this) }

    override val kotlinInstalls: KotlincInstalls by lazy { KotlincInstalls(this) }

    override val nodejsInstalls: NodejsInstalls by lazy { NodejsInstalls(this) }

    override val dokka: DokkaCli by lazy {
        DokkaCli(
            dokkaCliFolder = KTPACK_ROOT / "dokka",
            fs = SystemFs,
            http = http
        )
    }

    override val gitCli: GitCli = GitCli()

    override val rebuild: Boolean by option()
        .help("Force the pack script to be rebuilt, even if it has not changed.")
        .flag()

    override val stacktrace: Boolean by option("--stacktrace", "-s")
        .help("Print the stacktrace in the case of an unhandled exception.")
        .flag()
    override val debug: Boolean by option("--debug", "-d")
        .help("Print debug log statements to the console.")
        .flag()

    override fun aliases(): Map<String, List<String>> = emptyMap()

    override suspend fun loadKtpackConf(filePath: String): KtpackConf {
        return ktpack.script.loadKtpackConf(this, filePath, rebuild)
    }

    override val http: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            if (debug) {
                install(Logging) {
                    logger = io.ktor.client.plugins.logging.Logger.SIMPLE
                    level = LogLevel.INFO
                }
            }
        }
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
