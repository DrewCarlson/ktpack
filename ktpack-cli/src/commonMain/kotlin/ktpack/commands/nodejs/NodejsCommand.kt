package ktpack.commands.nodejs

import com.github.ajalt.clikt.core.*

class NodejsCommand : CliktCommand(name = "nodejs") {

    override fun help(context: Context): String {
        return context.theme.info("Install and manage Nodejs versions.")
    }

    override fun run() {
    }
}
