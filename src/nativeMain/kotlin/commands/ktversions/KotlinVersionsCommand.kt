package ktpack.commands.ktversions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import ktpack.util.*
import kotlin.system.*

class KotlinVersionsCommand(private val term: Terminal) : CliktCommand(
    name = "kotlin-versions",
    help = "Install and manage Kotlin Compiler versions.",
) {

    override fun run() {
    }
}
