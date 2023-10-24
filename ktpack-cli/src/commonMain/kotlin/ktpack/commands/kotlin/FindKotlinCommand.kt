package ktpack.commands.kotlin

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import ktpack.CliContext
import ktpack.util.*
import kotlin.system.*

private enum class Channel {
    RELEASE, RC, EAP, ALL
}

class FindKotlinCommand : CliktCommand(
    name = "find",
    help = "Find available Kotlin compiler versions.",
) {

    private val channel by option()
        .help("Filter results by the release channel.")
        .enum<Channel> { it.name.lowercase() }
        .default(Channel.RELEASE)

    private val context by requireObject<CliContext>()

    override fun run(): Unit = runBlocking {
        val releases = try {
            context.http.getCompilerReleases()
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
                            cell(release.tagName)
                        }
                    }
                }
            },
        )
    }
}

private fun List<GhRelease>.filterBy(channel: Channel) = filter { release ->
    when (channel) {
        Channel.ALL -> true
        Channel.RELEASE -> !release.tagName.contains("-")
        Channel.RC -> release.tagName.contains("-RC")
        Channel.EAP -> release.tagName.run {
            contains("-eap") || contains("-M")
        }
    }
}

private suspend fun HttpClient.getCompilerReleases(): List<GhRelease> {
    return get("https://api.github.com/repos/Jetbrains/kotlin/releases").body()
}
