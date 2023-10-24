package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import ktpack.configuration.KtpackConf
import ktpack.jdk.JdkInstalls
import ktpack.kotlin.KotlincInstalls
import ktpack.task.TaskRunner
import ktpack.util.GitCli

const val PACK_SCRIPT_FILENAME = "pack.kts"

interface CliContext {
    val stacktrace: Boolean
    val debug: Boolean
    val rebuild: Boolean
    val taskRunner: TaskRunner
    val http: HttpClient
    val term: Terminal
    val config: KtpackUserConfig
    val jdkInstalls: JdkInstalls
    val kotlinInstalls: KotlincInstalls
    val gitCli: GitCli

    suspend fun loadKtpackConf(filePath: String = PACK_SCRIPT_FILENAME): KtpackConf
}
