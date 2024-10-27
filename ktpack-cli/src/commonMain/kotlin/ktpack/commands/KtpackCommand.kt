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
import kotlinx.io.files.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import ktpack.*
import ktpack.compilation.dependencies.MavenDependencyResolver
import ktpack.compilation.tools.DokkaCli
import ktpack.toolchain.kotlin.KotlincInstalls
import ktpack.manifest.ManifestToml
import ktpack.manifest.toml
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.nodejs.NodejsInstalls
import ktpack.util.*

class KtpackCommand(
    override val term: Terminal,
) : CliktCommand(), CliContext {

    override fun help(context: Context): String {
        return context.theme.info("A simple tool for building and publishing Kotlin software.")
    }

    private val logger = Logger.forClass<KtpackCommand>()

    private val configPath = Path(KTPACK_ROOT, "config.json")
    private lateinit var _config: KtpackUserConfig

    override val config: KtpackUserConfig
        get() = _config

    override val jdkInstalls: JdkInstalls by lazy {
        JdkInstalls(config = config.jdk, http = http)
    }

    override val kotlinInstalls: KotlincInstalls by lazy {
        KotlincInstalls(
            config = config,
            http = http,
        )
    }

    override val nodejsInstalls: NodejsInstalls by lazy {
        NodejsInstalls(
            config = config,
            http = http,
        )
    }

    override val dokka: DokkaCli by lazy {
        DokkaCli(mavenResolver = MavenDependencyResolver(http))
    }

    override val gitCli: GitCli = GitCli()

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
        configPath.writeString(encodedConfig, ::logError)
    }

    override fun loadManifestToml(filePath: String): ManifestToml {
        val path = Path(filePath).let { path ->
            Path(workingDirectory, path.toString()).resolve()
        }
        check(path.exists()) { "No $MANIFEST_FILENAME file found in '${path.parent}'" }
        return toml.decodeFromString<ManifestToml>(path.readString())
            .resolveDependencyShorthand()
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
        loadConfig()
        currentContext.obj = this
        logger.d("${info("Ktpack")} ${verbose(Ktpack.VERSION)}")
    }

    private fun loadConfig() {
        _config = if (configPath.exists()) {
            logger.d("Loading existing KtpackUserConfig at $configPath")
            json.decodeFromString(configPath.readString())
        } else {
            logger.d("Creating default KtpackUserConfig at $configPath")
            check(KTPACK_ROOT.mkdirs().exists()) {
                "Failed to create Ktpack folder $KTPACK_ROOT"
            }
            val defaultConfig = KtpackUserConfig()
            val encodedConfig = jsonPretty.encodeToString(defaultConfig)
            configPath.writeString(encodedConfig, ::logError)
            defaultConfig
        }
    }
}
