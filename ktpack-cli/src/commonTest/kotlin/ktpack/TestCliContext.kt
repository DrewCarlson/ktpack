package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.serialization.decodeFromString
import ktpack.compilation.tools.DokkaCli
import ktpack.toolchain.kotlin.KotlincInstalls
import ktpack.manifest.ManifestToml
import ktpack.manifest.toml
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.nodejs.NodejsInstalls
import ktpack.util.*
import okio.Path.Companion.toPath

class TestCliContext : CliContext {
    override val stacktrace: Boolean = true
    override val debug: Boolean = true
    override val http: HttpClient = HttpClient {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }
    override val term: Terminal
        get() = TODO("Not yet implemented")
    override var config: KtpackUserConfig = KtpackUserConfig()
        private set
    override val jdkInstalls: JdkInstalls = JdkInstalls(this)
    override val kotlinInstalls: KotlincInstalls = KotlincInstalls(this)
    override val nodejsInstalls: NodejsInstalls = NodejsInstalls(this)
    override val dokka: DokkaCli by lazy {
        DokkaCli(
            dokkaCliFolder = KTPACK_ROOT / "dokka",
            fs = SystemFs,
            http = http
        )
    }
    override val gitCli: GitCli = GitCli()

    override fun updateConfig(body: KtpackUserConfig.() -> KtpackUserConfig) {
        config = config.run(body)
    }

    override fun loadManifestToml(filePath: String): ManifestToml {
        val path = filePath.toPath().let { path ->
            if (path.isRelative) {
                workingDirectory.resolve(path, normalize = true)
            } else {
                path
            }
        }
        check(path.exists()) {
            "No $MANIFEST_FILENAME file found in '${path.parent}'"
        }
        return toml.decodeFromString<ManifestToml>(path.readUtf8())
            .resolveDependencyShorthand()
    }
}
