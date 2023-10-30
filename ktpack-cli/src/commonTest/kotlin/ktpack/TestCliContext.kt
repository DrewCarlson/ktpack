package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import ktpack.kotlin.KotlincInstalls
import ktpack.configuration.KtpackConf
import ktpack.jdk.JdkInstalls
import ktpack.task.TaskRunner
import ktpack.util.GitCli

class TestCliContext : CliContext {
    override val stacktrace: Boolean = true
    override val debug: Boolean = true
    override val rebuild: Boolean = true
    override val taskRunner: TaskRunner = TaskRunner()
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
    override val gitCli: GitCli = GitCli()

    override suspend fun loadKtpackConf(filePath: String): KtpackConf {
        return ktpack.script.loadKtpackConf(this, filePath, rebuild)
    }
}
