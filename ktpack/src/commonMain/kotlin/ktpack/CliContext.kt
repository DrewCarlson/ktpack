package ktpack

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.HttpClient
import ktpack.commands.jdk.JdkInstalls
import ktpack.configuration.PackageConf
import ktpack.task.TaskRunner

interface CliContext {
    val stacktrace: Boolean
    val debug: Boolean
    val taskRunner: TaskRunner
    val http: HttpClient
    val term: Terminal
    val config: KtpackUserConfig
    val jdkInstalls: JdkInstalls

    suspend fun loadPackage(
        filePath: String = PACK_SCRIPT_FILENAME,
        forceRebuild: Boolean = false,
    ): PackageConf
}
