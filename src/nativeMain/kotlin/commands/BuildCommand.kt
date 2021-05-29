package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.*
import com.github.ajalt.mordant.rendering.TextColors.*
import ktpack.*
import ktpack.subprocess.*
import me.archinamon.fileio.*
import kotlin.math.*
import kotlin.time.*
import kotlin.time.DurationUnit.*

class BuildCommand(private val term: Terminal) : CliktCommand(
    help = "Compile packages and dependencies.",
) {
    override fun run() {
        val manifest = loadManifest(MANIFEST_NAME)
        val module = manifest.module

        val srcFolder = File("src")
        check(srcFolder.isDirectory()) { "Expected `src` file to be a directory." }

        if (module.autobin) {
            val mainSource = srcFolder.listFiles().find { it.getName() == "main.kt" }
            if (mainSource == null) {
                //TODO("Only `src/main.kt` autobin is supported at this time.")
            } else {
                val outDir = File("out/")
                if (!outDir.exists()) outDir.mkdirs()

                val sourceFiles = srcFolder.walkTopDown().toList()
                    .filter { it.getAbsolutePath() != mainSource.getAbsolutePath() }

                term.println("${brightGreen("Compiling")} ${module.name} v${module.version} (${srcFolder.getParent()})")
                val compileDuration = measureTime {
                    exec {
                        // TODO: actual kotlinc lookup and default selection behavior kotlinc/kotlinc-native
                        arg("/usr/local/bin/kotlinc-native")
                        //arg("%homepath%/.konan/kotlin-native-prebuilt-windows-1.5.10/bin/kotlinc-native.bat")
                        // TODO: actual search for main functions and output details
                        arg(mainSource.getAbsolutePath())
                        sourceFiles.forEach { file ->
                            arg(file.getAbsolutePath())
                        }
                        arg("-o")
                        arg("out/main")
                    }
                }.toDouble(SECONDS).roundTo(2)
                term.println("${brightGreen("Finished")} dev [unoptimized + debuginfo] target(s) in ${compileDuration}s")
            }
        }
    }
}

fun Double.roundTo(numFractionDigits: Int): Double {
    val factor = 10.0.pow(numFractionDigits.toDouble())
    return (this * factor).roundToInt() / factor
}
