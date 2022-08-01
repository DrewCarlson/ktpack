package ktpack.manifest

import com.appmattus.crypto.Algorithm
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import ksubprocess.Process
import ktfio.*
import ktpack.CliContext
import ktpack.commands.kotlin.KotlincInstalls
import ktpack.configuration.ManifestConf
import ktpack.configuration.ModuleConf
import ktpack.json
import ktpack.ktpackManifestJarPath
import ktpack.ktpackManifestJarUrl
import ktpack.util.TEMP_DIR
import ktpack.util.measureSeconds

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

suspend fun loadManifest(context: CliContext, path: String, rebuild: Boolean): ManifestConf {
    val digest = Algorithm.SHA_256.createDigest().apply { update(File(path).readBytes()) }.digest().toHexString()
    val cacheKey = File(TEMP_DIR, ".ktpack-manifest-cache-$digest")
    val (module, duration) = measureSeconds {
        if (cacheKey.exists() && rebuild) cacheKey.delete()
        if (cacheKey.exists()) {
            //println("Reading manifest from cache")
            json.decodeFromString(cacheKey.readText())
        } else {
            //println("Processing manifest")
            // TODO: Log in debug only
            Dispatchers.Default { executePackage(context, path) }.also { manifestConf ->
                //println(cacheKey.getAbsolutePath())
                if (cacheKey.createNewFile()) {
                    //println("Caching new manifest output")
                    cacheKey.writeText(json.encodeToString(manifestConf))
                }
            }
        }
    }
    println("Manifest loaded in ${duration}s: $path")
    return module
}

private fun ByteArray.toHexString(): String {
    return joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }
}

private suspend fun executePackage(context: CliContext, path: String): ManifestConf = coroutineScope {
    installManifestBuilderJar(context)

    val moduleConf = Process {
        arg(KotlincInstalls.findKotlincJvm(context.config.kotlinVersion))
        args("-classpath", ktpackManifestJarPath.getAbsolutePath())
        args("-script-templates", "ktpack.configuration.PackageScriptDefinition")
        arg("-script")
        arg(path)
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

private suspend fun installManifestBuilderJar(context: CliContext) {
    if (ktpackManifestJarPath.exists()) return
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
