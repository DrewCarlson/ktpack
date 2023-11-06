package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import ktpack.compilation.tools.DokkaCli
import ktpack.configuration.KtpackConf
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.kotlin.KotlincInstalls
import ktpack.toolchain.nodejs.NodejsInstalls
import ktpack.util.GitCli

const val PACK_SCRIPT_FILENAME = "pack.kts"

interface CliContext {

    /**
     * When true, print the full stacktrace in the case of an uncaught
     * exception.
     */
    val stacktrace: Boolean

    /**
     * Enable all logging channels, normally only info messages are logged.
     */
    val debug: Boolean

    /**
     * When true, all pack.kts scripts will be rebuilt before use.
     */
    val rebuild: Boolean
    val http: HttpClient
    val term: Terminal
    val config: KtpackUserConfig
    val jdkInstalls: JdkInstalls
    val kotlinInstalls: KotlincInstalls
    val nodejsInstalls: NodejsInstalls
    val dokka: DokkaCli
    val gitCli: GitCli

    fun updateConfig(body: KtpackUserConfig.() -> KtpackUserConfig)

    /**
     * Load the [KtpackConf] for the pack.kts file at [filePath].
     * If [filePath] is a relative, it will be resolved to the current
     * working directory.
     */
    suspend fun loadKtpackConf(filePath: String = PACK_SCRIPT_FILENAME): KtpackConf
}
