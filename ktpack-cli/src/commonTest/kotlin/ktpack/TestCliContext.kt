package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import kotlinx.io.files.Path
import kotlinx.serialization.decodeFromString
import ktpack.compilation.dependencies.MavenDependencyResolver
import ktpack.compilation.tools.DokkaCli
import ktpack.toolchain.kotlin.KotlincInstalls
import ktpack.manifest.ManifestToml
import ktpack.manifest.toml
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.nodejs.NodejsInstalls
import ktpack.util.*
import kotlin.time.Duration.Companion.seconds

class TestCliContext : CliContext {
    override val stacktrace: Boolean = true
    override val debug: Boolean = true
    override val http: HttpClient = HttpClient {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        install(HttpTimeout) {
            socketTimeoutMillis = 60.seconds.inWholeMilliseconds
            requestTimeoutMillis = 60.seconds.inWholeMilliseconds
        }
    }
    override val term: Terminal
        get() = TODO("Not yet implemented")
    override var config: KtpackUserConfig = KtpackUserConfig()
        private set
    override val jdkInstalls: JdkInstalls by lazy {
        JdkInstalls(config = config.jdk, http = http)
    }
    override val kotlinInstalls: KotlincInstalls by lazy {
        KotlincInstalls(config = config, http = http)
    }
    override val nodejsInstalls: NodejsInstalls by lazy {
        NodejsInstalls(config = config, http = http)
    }
    override val dokka: DokkaCli by lazy {
        DokkaCli(
            mavenResolver = MavenDependencyResolver(http)
        )
    }
    override val gitCli: GitCli = GitCli()

    override fun updateConfig(body: KtpackUserConfig.() -> KtpackUserConfig) {
        config = config.run(body)
    }

    override fun loadManifestToml(filePath: String): ManifestToml {
        val path = Path(filePath).resolve()
        check(path.exists()) {
            "No $MANIFEST_FILENAME file found in '${path.parent}'"
        }
        return toml.decodeFromString<ManifestToml>(path.readString())
            .resolveDependencyShorthand()
    }
}
