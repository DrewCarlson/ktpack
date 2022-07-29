package ktpack

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import ksubprocess.Process
import ktfio.appendBytes
import ktpack.commands.kotlin.KotlincInstalls
import ktpack.configuration.ManifestConf
import ktpack.configuration.ModuleConf
import ktpack.util.measureSeconds

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

suspend fun executePackage(context: CliContext, path: String): ManifestConf = coroutineScope {
    if (!ktpackManifestJarPath.exists()) {
        println("Manifest processor jar does not exist, creating it at: ${ktpackManifestJarPath.getAbsolutePath()}")
        ktpackManifestJarPath.getParentFile()?.mkdirs()
        val (_, duration) = measureSeconds {
            if (!ktpackManifestJarPath.exists() && ktpackManifestJarPath.createNewFile()) {
                println("Downloading dependency: $ktpackManifestJarUrl")
                val response = context.http.prepareGet(ktpackManifestJarUrl).execute { response ->
                    val body = response.bodyAsChannel()
                    while (!body.isClosedForRead) {
                        val packet = body.readRemaining(DOWNLOAD_BUFFER_SIZE)
                        while (packet.isNotEmpty) {
                            ktpackManifestJarPath.appendBytes(packet.readBytes())
                        }
                    }
                    response
                }
                if (!response.status.isSuccess()) {
                    ktpackManifestJarPath.delete()
                    error("Failed to download dependency: ${response.status} $ktpackManifestJarUrl")
                }
            }
        }
        println("Manifest process jar written in ${duration}s")
    }

    val moduleConf = Process {
        arg(KotlincInstalls.findKotlincJvm("1.7.10"))
        args("-classpath", ktpackManifestJarPath.getAbsolutePath())
        args("-jdk-home", checkNotNull(context.jdkInstalls.getDefaultJdk()?.path))
        arg(path)
        arg("-script")
        if (context.debug) {
            println("Processing package script:")
            println(arguments.joinToString(" "))
        }
    }.run {
        if (context.debug) {
            stderrLines.onEach { println(it) }.launchIn(this@coroutineScope)
        }
        stdoutLines
            .run { if (context.debug) onEach { println(it) } else this }
            .mapNotNull { it.substringAfter("ktpack-module:", "").takeUnless(String::isBlank) }
            .map { json.decodeFromString<ModuleConf>(it) }
            .toList()
    }.firstOrNull() ?: error("No modules declared in $path")

    ManifestConf(moduleConf)
}
