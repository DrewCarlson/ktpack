package ktpack.commands.jdk

import com.github.ajalt.clikt.core.*
import kotlinx.coroutines.*
import ktpack.CliContext

class FindJdkCommand : CliktCommand(name = "find") {

    override fun help(context: Context): String {
        return context.theme.info("Find JDK versions to add.")
    }

    private val context by requireObject<CliContext>()

    override fun run(): Unit = runBlocking {
        /*val releases = try {
            http.getCompilerReleases()
        } catch (e: Throwable) {
            term.println("${failed("Error")} ${e.message}")
            exitProcess(1)
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
