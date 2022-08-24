package ktpack.script

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
import ktpack.configuration.KtpackConf
import ktpack.configuration.ModuleConf
import ktpack.json
import ktpack.ktpackScriptJarPath
import ktpack.ktpackScriptJarUrl
import ktpack.util.TEMP_DIR
import ktpack.util.measureSeconds

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

suspend fun loadKtpackConf(context: CliContext, path: String, rebuild: Boolean): KtpackConf {
    val digest = Algorithm.SHA_256.createDigest().apply { update(File(path).readBytes()) }.digest().toHexString()
    val cacheKey = TEMP_DIR.nestedFile(".ktpack-script-cache-$digest")
    val (module, duration) = measureSeconds {
        if (cacheKey.exists() && rebuild) cacheKey.delete()
        if (cacheKey.exists()) {
            // println("Reading manifest from cache")
            json.decodeFromString(cacheKey.readText())
        } else {
            // println("Processing manifest")
            // TODO: Log in debug only
            Dispatchers.Default { executeKtpackScript(context, path) }.also { packageConf ->
                // println(cacheKey.getAbsolutePath())
                if (cacheKey.createNewFile()) {
                    // println("Caching new manifest output")
                    cacheKey.writeText(json.encodeToString(packageConf))
                }
            }
        }
    }
    println("Ktpack Script loaded in ${duration}s: $path")
    return module
}

private fun ByteArray.toHexString(): String {
    return joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }
}

private suspend fun executeKtpackScript(context: CliContext, path: String): KtpackConf = coroutineScope {
    installScriptBuilderJar(context)

    val kotlincPath = context.kotlinInstalls.findKotlincJvm(context.config.kotlin.version)
    check(File(kotlincPath).exists()) { "Cannot execute ktpack script, kotlinc-jvm does not exist at: $kotlincPath" }
    val moduleConf = Process {
        arg(kotlincPath)
        if (context.debug) arg("-verbose")
        args("-classpath", ktpackScriptJarPath.getAbsolutePath())
        args("-script-templates", "ktpack.configuration.KtpackScriptScopeDefinition")
        arg("-script")
        arg(path)
        if (context.debug) {
            println("Processing ktpack script:")
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

    KtpackConf(moduleConf)
}

private suspend fun installScriptBuilderJar(context: CliContext) {
    if (ktpackScriptJarPath.exists()) return
    println("Package Script jar does not exist, creating it at: ${ktpackScriptJarPath.getAbsolutePath()}")
    ktpackScriptJarPath.getParentFile()?.mkdirs()
    val (_, duration) = measureSeconds {
        if (!ktpackScriptJarPath.exists() && ktpackScriptJarPath.createNewFile()) {
            println("Downloading dependency: $ktpackScriptJarUrl")
            val response = context.http.prepareGet(ktpackScriptJarUrl).execute { response ->
                val body = response.bodyAsChannel()
                while (!body.isClosedForRead) {
                    val packet = body.readRemaining(DOWNLOAD_BUFFER_SIZE)
                    while (packet.isNotEmpty) {
                        ktpackScriptJarPath.appendBytes(packet.readBytes())
                    }
                }
                response
            }
            if (!response.status.isSuccess()) {
                ktpackScriptJarPath.delete()
                error("Failed to download dependency: ${response.status} $ktpackScriptJarUrl")
            }
        }
    }
    println("Ktpack Script jar written in ${duration}s")
}
