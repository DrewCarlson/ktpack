package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.*
import ktpack.commands.jdk.JdkInstalls
import ktpack.commands.kotlin.KotlincInstalls
import ktpack.configuration.KtpackConf
import ktpack.task.TaskRunner
import ktpack.util.GitCli

class TestCliContext : CliContext {
    override val stacktrace: Boolean = true
    override val debug: Boolean = true
    override val rebuild: Boolean = true
    override val taskRunner: TaskRunner = TaskRunner()
    override val http: HttpClient = HttpClient()
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
