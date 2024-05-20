package ktpack.commands.kotlin

import com.github.ajalt.clikt.core.*

class KotlinCommand : CliktCommand(name = "kotlin") {

    override fun help(context: Context): String {
        return context.theme.info("Install and manage Kotlin Compiler versions.")
    }

    override fun run() {
    }
}
