package ktpack.commands.jdk

import com.github.ajalt.clikt.core.*

class JdkCommand : CliktCommand(name = "jdk") {

    override fun help(context: Context): String {
        return context.theme.info("Install and manage JDK versions.")
    }

    override fun run() {
    }
}
