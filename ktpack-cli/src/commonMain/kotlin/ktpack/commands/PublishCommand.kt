package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.theme
import kotlinx.coroutines.runBlocking
import ktpack.CliContext
import ktpack.util.failed

class PublishCommand : CliktCommand(name = "publish") {
    override fun help(context: Context): String {
        return context.theme.info("Publish library and application outputs.")
    }

    private val context by requireObject<CliContext>()
    private val logger = Logger.withTag(PublishCommand::class.simpleName.orEmpty())

    override fun run(): Unit = runBlocking {
        logger.i("${failed("Publish")} Publishing is not implemented yet.")
    }
}
