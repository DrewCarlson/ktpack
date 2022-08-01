package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import ktpack.commands.jdk.JdkInstalls
import ktpack.configuration.ManifestConf
import ktpack.task.TaskRunner

interface CliContext {
    val stacktrace: Boolean
    val debug: Boolean
    val taskRunner: TaskRunner
    val http: HttpClient
    val term: Terminal
    val config: KtpackUserConfig
    val jdkInstalls: JdkInstalls

    suspend fun loadManifest(
        filePath: String = MANIFEST_NAME,
        forceRebuild: Boolean = false,
    ): ManifestConf
}