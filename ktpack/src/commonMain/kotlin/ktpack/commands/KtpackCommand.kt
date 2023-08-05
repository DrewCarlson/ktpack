package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import ktfio.File
import ktfio.readText
import ktpack.*
import ktpack.commands.jdk.JdkInstalls
import ktpack.commands.kotlin.KotlincInstalls
import ktpack.configuration.KtpackConf
import ktpack.task.TaskRunner
import ktpack.util.GitCli
import ktpack.util.KTPACK_ROOT
import ktpack.util.info
import ktpack.util.verbose
import okio.FileSystem
import okio.Path.Companion.toPath

class KtpackCommand(
    override val term: Terminal,
) : CliktCommand(
    help = "A simple tool for building and publishing Kotlin software.",
),
    CliContext {

    override val http: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    override val config: KtpackUserConfig by lazy {
        File(KTPACK_ROOT, "config.json").run {
            if (!exists()) {
                FileSystem.SYSTEM.createDirectory(KTPACK_ROOT.toPath(), mustCreate = false)
                // check(File(KTPACK_ROOT).mkdirs()) {
                //    "Failed to create Ktpack folder $KTPACK_ROOT"
                // }
                println(getAbsolutePath())
                FileSystem.SYSTEM.write("${KTPACK_ROOT}/config.json".toPath(true), true) {
                    write(json.encodeToString(KtpackUserConfig()).toByteArray())
                }
            }

            json.decodeFromString(readText())
        }
    }

    override val jdkInstalls: JdkInstalls by lazy { JdkInstalls(this) }

    override val kotlinInstalls: KotlincInstalls by lazy { KotlincInstalls(this) }

    override val gitCli: GitCli = GitCli()

    override val stacktrace: Boolean by option()
        .help("Print the stacktrace in the case of an unhandled exception.")
        .flag()
    override val debug: Boolean by option().flag()

    override fun aliases(): Map<String, List<String>> = emptyMap()

    override val taskRunner: TaskRunner = TaskRunner()

    override suspend fun loadKtpackConf(filePath: String, forceRebuild: Boolean): KtpackConf {
        return ktpack.script.loadKtpackConf(this, filePath, forceRebuild)
    }

    override fun run() {
        currentContext.obj = this
        if (debug) {
            term.println("${info("Ktpack")} ${verbose(Ktpack.VERSION)}")
        }
    }
}
