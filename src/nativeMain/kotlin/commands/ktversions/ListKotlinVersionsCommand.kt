package ktpack.commands.ktversions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import ktpack.util.*
import kotlin.system.*

private enum class Channel {
    RELEASE, RC, EAP,
}

class ListKotlinVersionsCommand(
    private val term: Terminal,
    private val http: HttpClient,
) : CliktCommand(
    name = "list",
    help = "Manage and install Kotlin Compiler versions.",
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

        term.println(title("Available Versions:"))
        releases
            .filterBy(channel)
            .forEach { release ->
                term.println(" ${release.tagName}")
            }
    }
}

private fun List<GhRelease>.filterBy(channel: Channel) = filter { release ->
    when (channel) {
        Channel.RC -> release.tagName.contains("-RC")
        Channel.EAP -> release.tagName.run {
            contains("-eap") || contains("-M")
        }
        Channel.RELEASE -> !release.tagName.contains("-")
    }
}

private suspend fun HttpClient.getCompilerReleases(): List<GhRelease> {
    return get("https://api.github.com/repos/Jetbrains/kotlin/releases")
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
    val browserDownloadUrl: String,
)
