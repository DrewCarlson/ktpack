package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import ktpack.compilation.tools.DokkaCli
import ktpack.manifest.ManifestToml
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.kotlin.KotlincInstalls
import ktpack.toolchain.nodejs.NodejsInstalls
import ktpack.util.GitCli

const val MANIFEST_FILENAME = "pack.toml"

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

    val http: HttpClient
    val term: Terminal
    val config: KtpackUserConfig
    val jdkInstalls: JdkInstalls
    val kotlinInstalls: KotlincInstalls
    val nodejsInstalls: NodejsInstalls
    val dokka: DokkaCli
    val gitCli: GitCli

    fun updateConfig(body: KtpackUserConfig.() -> KtpackUserConfig)

    fun loadManifestToml(filePath: String = MANIFEST_FILENAME): ManifestToml
}
