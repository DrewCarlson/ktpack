package ktpack.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import kotlinx.coroutines.runBlocking
import ktpack.CliContext

class PublishCommand : CliktCommand(
    help = "Publish library and application outputs.",
) {
    private val context by requireObject<CliContext>()

    override fun run(): Unit = runBlocking {
        TODO("Not implemented")
    }
}
