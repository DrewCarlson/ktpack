package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import ktpack.compilation.BuildContext
import ktpack.compilation.dependencies.MavenDependencyResolver
import ktpack.compilation.tools.DokkaCli
import ktpack.manifest.ManifestLoader
import ktpack.toolchain.jdk.JdkInstalls
import ktpack.toolchain.kotlin.KotlincInstalls
import ktpack.toolchain.nodejs.NodejsInstalls
import ktpack.util.GitCli

interface CliContext : ManifestLoader {

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

    fun createBuildContext(): BuildContext {
        return BuildContext(
            manifestLoader = this,
            resolver = MavenDependencyResolver(http),
            jdk = jdkInstalls,
            kotlinc = kotlinInstalls,
            debug = debug,
        )
    }

    fun updateConfig(body: KtpackUserConfig.() -> KtpackUserConfig)
}
