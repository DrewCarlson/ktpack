package ktpack.commands.kotlin

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import kotlinx.coroutines.*
import ktpack.CliContext
import ktpack.github.models.GhTag
import ktpack.util.*

private enum class Channel {
    RELEASE, RC, BETA, EAP, ALL
}

class FindKotlinCommand : CliktCommand(name = "find") {

    override fun help(context: Context): String {
        return context.theme.info("Find available Kotlin compiler versions.")
    }

    private val context by requireObject<CliContext>()

    private val channel by option("--chanel", "-c")
        .help("Filter results by the release channel.")
        .enum<Channel> { it.name.lowercase() }
        .default(Channel.RELEASE)

    private val page by option("--page", "-p")
        .help("The page of versions to load from github, default is 1 and results start with the latest version.")
        .int()
        .default(1)

    override fun run(): Unit = runBlocking {
        val releases = try {
            context.kotlinInstalls.getCompilerReleases(page)
        } catch (e: Throwable) {
            context.term.println("${failed("Error")} ${e.message}")
            exitProcess(1)
        }

        val selectedReleases = releases.filterBy(channel)
        context.term.println(
            table {
                cellBorders = Borders.NONE
                padding(0)
                captionTop(title("Kotlin Compiler Versions"))
                body {
                    selectedReleases.forEach { release ->
                        row {
                            cell(" ")
                            cell(release.name)
                        }
                    }
                }
            },
        )
    }
}

private fun List<GhTag>.filterBy(channel: Channel) = filter { release ->
    when (channel) {
        Channel.ALL -> true
        Channel.RELEASE -> !release.name.contains("-")
        Channel.RC -> release.name.contains("-RC")
        Channel.BETA -> release.name.contains("-Beta")
        Channel.EAP -> release.name.run {
            contains("-eap") || contains("-M")
        }
    }
}
