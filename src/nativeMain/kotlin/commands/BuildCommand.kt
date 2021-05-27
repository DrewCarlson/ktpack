package ktpack.commands

import com.github.ajalt.clikt.core.*
import ktpack.*
import ktpack.subprocess.*

class BuildCommand : CliktCommand(
    help = "Compile packages and dependencies.",
) {
    override fun run() {
        val manifest = loadManifest(MANIFEST_NAME)

        val result = exec {
            // TODO: actual kotlinc lookup and default selection behavior kotlinc/kotlinc-native
            arg("/usr/local/bin/kotlinc-native")
            //arg("%homepath%/.konan/kotlin-native-prebuilt-windows-1.5.10/bin/kotlinc-native.bat")
            // TODO: actual search for main functions and output details
            arg("src/main.kt")
            arg("-o")
            arg("out/main")
        }
    }
}
