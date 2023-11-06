package ktpack.commands

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
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

class KtpackCommand(
    override val term: Terminal,
) : CliktCommand(
    help = "A simple tool for building and publishing Kotlin software.",
),
    CliContext {

    private val logger = Logger.withTag(KtpackCommand::class.simpleName.orEmpty())

    private val configPath = KTPACK_ROOT / "config.json"
    private var _config: KtpackUserConfig

    init {
        _config = if (configPath.exists()) {
            logger.d("Loading existing KtpackUserConfig at $configPath")
            json.decodeFromString(configPath.readUtf8())
        } else {
            logger.d("Creating default KtpackUserConfig at $configPath")
            check(KTPACK_ROOT.mkdirs().exists()) {
                "Failed to create Ktpack folder $KTPACK_ROOT"
            }
            val defaultConfig = KtpackUserConfig()
            val encodedConfig = jsonPretty.encodeToString(defaultConfig)
            configPath.writeUtf8(encodedConfig, ::logError)
            defaultConfig
        }
    }

    override val config: KtpackUserConfig
        get() = _config

    override val jdkInstalls: JdkInstalls by lazy { JdkInstalls(this) }

    override val kotlinInstalls: KotlincInstalls by lazy { KotlincInstalls(this) }

    override val nodejsInstalls: NodejsInstalls by lazy { NodejsInstalls(this) }

    override val dokka: DokkaCli by lazy {
        DokkaCli(
            dokkaCliFolder = KTPACK_ROOT / "dokka",
            fs = SystemFs,
            http = http,
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

    override fun updateConfig(body: KtpackUserConfig.() -> KtpackUserConfig) {
        _config = _config.run(body)
        logger.d("Updating KtpackUserConfig at $configPath")
        check(KTPACK_ROOT.mkdirs().exists()) {
            "Failed to create Ktpack folder $KTPACK_ROOT"
        }
        val encodedConfig = jsonPretty.encodeToString(_config)
        configPath.writeUtf8(encodedConfig, ::logError)
    }

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
        Logger.mutableConfig.minSeverity = if (debug) Severity.Verbose else Severity.Info
        Logger.addLogWriter(MordantLogWriter(term = term))
        currentContext.obj = this
        if (debug) {
            term.println("${info("Ktpack")} ${verbose(Ktpack.VERSION)}")
        }
    }
}
