package ktpack.commands.ktversions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import ktpack.util.*
import kotlin.system.*

private enum class Channel {
    RELEASE, RC, EAP, ALL
}

class ListKotlinVersionsCommand(
    private val term: Terminal,
    private val http: HttpClient,
) : CliktCommand(
    name = "list",
    help = "List available and installed Kotlin compiler versions.",
) {

    private val channel by option()
        .help("Filter results by the release channel.")
        .enum<Channel> { it.name.lowercase() }
        .default(Channel.RELEASE)

    override fun run(): Unit = runBlocking {
        val releases = try {
            http.getCompilerReleases()
        } catch (e: Throwable) {
            term.println("${failed("Error")} ${e.message}")
            exitProcess(-1)
        }

        val selectedReleases = releases.filterBy(channel)
        term.println(
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
            }
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

@Serializable
private data class GhRelease(
    @SerialName("tag_name")
    val tagName: String,
    val assets: List<GhAsset>,
)

@Serializable
private data class GhAsset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url")
    val downloadUrl: String,
)
