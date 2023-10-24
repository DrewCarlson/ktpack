package ktpack.script

import com.appmattus.crypto.Algorithm
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import kotlinx.serialization.encodeToString
import ksubprocess.Process
import ktpack.CliContext
import ktpack.configuration.KtpackConf
import ktpack.configuration.ModuleConf
import ktpack.json
import ktpack.ktpackScriptJarPath
import ktpack.ktpackScriptJarUrl
import ktpack.util.*
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.system.exitProcess

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

suspend fun loadKtpackConf(context: CliContext, path: String, rebuild: Boolean): KtpackConf {
    fun logDebug(message: String) {
        if (context.debug) println(message)
    }

    val digest = Algorithm.SHA_256.createDigest()
        .apply { update(path.toPath().readByteArray()) }
        .digest()
        .toHexString()
    val cacheKey = TEMP_PATH / ".ktpack-script-cache-$digest"
    val (module, duration) = measureSeconds {
        if (rebuild && cacheKey.exists()) {
            logDebug("Rebuilding cached pack script: $cacheKey")
            cacheKey.delete()
        }
        if (cacheKey.exists()) {
            logDebug("Reading manifest from cache")
            json.decodeFromString(cacheKey.readUtf8())
        } else {
            logDebug("Processing manifest")
            Dispatchers.Default { executeKtpackScript(context, path) }.also { packageConf ->
                logDebug(cacheKey.toString())
                if (cacheKey.createNewFile()) {
                    logDebug("Caching new manifest output")
                    cacheKey.writeUtf8(json.encodeToString(packageConf)) { error ->
                        logDebug("Failed to write packageConf: ${error.message}")
                        if (context.stacktrace) {
                            error.printStack()
                            exitProcess(1)
                        }
                    }
                }
            }
        }
    }
    logDebug("Ktpack Script loaded in ${duration}s: $path")
    return module
}

private fun ByteArray.toHexString(): String {
    return joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }
}

private suspend fun executeKtpackScript(context: CliContext, path: String): KtpackConf = coroutineScope {
    installScriptBuilderJar(context)

    val kotlincPath = context.kotlinInstalls.findKotlincJvm(context.config.kotlin.version)
    check(kotlincPath.toPath().exists()) {
        "Cannot execute ktpack script, kotlinc-jvm does not exist at: $kotlincPath"
    }
    val moduleConf = Process {
        arg(kotlincPath)
        if (context.debug) arg("-verbose")
        args("-classpath", ktpackScriptJarPath.toString())
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
    println("Package Script jar does not exist, creating it at: $ktpackScriptJarPath")
    ktpackScriptJarPath.parent?.mkdirs()
    val (_, duration) = measureSeconds {
        if (!ktpackScriptJarPath.exists() && ktpackScriptJarPath.createNewFile()) {
            println("Downloading dependency: $ktpackScriptJarUrl")
            val response = context.http.prepareGet(ktpackScriptJarUrl).execute { response ->
                val body = response.bodyAsChannel()
                val sink = FileSystem.SYSTEM.appendingSink(ktpackScriptJarPath)
                val bufferedSink = sink.buffer()
                try {
                    while (!body.isClosedForRead) {
                        val packet = body.readRemaining(DOWNLOAD_BUFFER_SIZE)
                        while (packet.isNotEmpty) {
                            bufferedSink.write(packet.readBytes())
                        }
                    }
                } finally {
                    bufferedSink.close()
                    sink.close()
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
