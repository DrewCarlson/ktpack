package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import ktpack.compilation.tools.DokkaCli
import ktpack.toolchain.kotlin.KotlincInstalls
import ktpack.configuration.KtpackConf
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.nodejs.NodejsInstalls
import ktpack.util.GitCli
import ktpack.util.KTPACK_ROOT
import ktpack.util.SystemFs

class TestCliContext : CliContext {
    override val stacktrace: Boolean = true
    override val debug: Boolean = true
    override val rebuild: Boolean = true
    override val http: HttpClient = HttpClient {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }
    override val term: Terminal
        get() = TODO("Not yet implemented")
    override val config: KtpackUserConfig = KtpackUserConfig()
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

    override suspend fun loadKtpackConf(filePath: String): KtpackConf {
        return ktpack.script.loadKtpackConf(this, filePath, rebuild)
    }
}
