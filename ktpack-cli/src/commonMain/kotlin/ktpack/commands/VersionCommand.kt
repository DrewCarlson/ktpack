package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import ktpack.CliContext
import ktpack.Ktpack
import ktpack.util.info
import ktpack.util.verbose

class VersionCommand : CliktCommand() {
    override fun help(context: Context): String {
        return context.theme.info("Show Ktpack version information.")
    }

    private val context by requireObject<CliContext>()
    private val logger = Logger.withTag(VersionCommand::class.simpleName.orEmpty())

    override fun run() {
        logger.i("${info("Ktpack")} v${verbose(Ktpack.VERSION)}")
        logger.i("${info("Kotlin")} v${verbose(Ktpack.KOTLIN_VERSION)}")
        logger.i("${info("Coroutines")} v${verbose(Ktpack.COROUTINES_VERSION)}")
        logger.i("${info("Ktor")} v${verbose(Ktpack.KTOR_VERSION)}")
        logger.i("${info("Serialization")} v${verbose(Ktpack.SERIALIZATION_VERSION)}")
        logger.i("${info("Build sha")} ${verbose(Ktpack.BUILD_SHA)}")
        logger.i("${info("Build date")} ${verbose(Ktpack.BUILD_DATE)}")
    }
}
