package ktpack.commands.jdkversions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import kotlinx.coroutines.*
import ktpack.KtpackContext

class FindJdkVersionsCommand : CliktCommand(
    name = "find",
    help = "Find Jdk versions to add.",
) {

    private val context by requireObject<KtpackContext>()

    override fun run(): Unit = runBlocking {
        /*val releases = try {
            http.getCompilerReleases()
        } catch (e: Throwable) {
            term.println("${failed("Error")} ${e.message}")
            exitProcess(-1)
        }

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
        )*/
    }
}
