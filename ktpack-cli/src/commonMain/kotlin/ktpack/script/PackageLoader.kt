package ktpack.script

import co.touchlab.kermit.Logger
import com.appmattus.crypto.Algorithm
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import kotlinx.serialization.encodeToString
import ksubprocess.Process
import ktpack.*
import ktpack.configuration.KtpackConf
import ktpack.configuration.ModuleConf
import ktpack.util.*
import okio.Path.Companion.toPath

private val logger by lazy { Logger.withTag("Package Loader") }

suspend fun loadKtpackConf(context: CliContext, pathString: String, rebuild: Boolean): KtpackConf {
    // Resolve relative pathString values
    val path = pathString.toPath()
        .let { path ->
            if (path.isRelative) {
                workingDirectory.resolve(path, normalize = true)
            } else {
                path
            }
        }
    check(path.exists()) {
        "No $PACK_SCRIPT_FILENAME file found in '${path.parent}'"
    }

    val digest = Algorithm.SHA_256.createDigest()
        .apply { update(path.readByteArray()) }
        .digest()
        .toHexString()
    val cacheKey = TEMP_PATH / ".ktpack-script-cache-$digest"
    val (module, duration) = measureSeconds {
        if (rebuild && cacheKey.exists()) {
            logger.d { "Rebuilding cached pack script: $cacheKey" }
            cacheKey.delete()
        }
        if (cacheKey.exists()) {
            logger.d { "Reading manifest from cache" }
            json.decodeFromString(cacheKey.readUtf8())
        } else {
            logger.d { "Processing manifest" }
            val packageConf = Dispatchers.Default { executeKtpackScript(context, path.toString()) }
            if (cacheKey.createNewFile()) {
                logger.d { "Caching new manifest output $cacheKey" }
                cacheKey.writeUtf8(json.encodeToString(packageConf)) { error ->
                    logger.d { "Failed to write packageConf: ${error.message}" }
                    if (context.stacktrace) {
                        error.printStackTrace()
                        exitProcess(1)
                    }
                }
            }
            packageConf
        }
    }
    logger.d { "Ktpack Script loaded in ${duration}s: $path" }
    return module
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

        logger.d { "Processing ktpack script:\n${arguments.joinToString("\n")}" }
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
    logger.d { "Package Script jar does not exist, creating it at: $ktpackScriptJarPath" }
    ktpackScriptJarPath.parent?.mkdirs()
    val (_, duration) = measureSeconds {
        if (!ktpackScriptJarPath.exists() && ktpackScriptJarPath.createNewFile()) {
            logger.d { "Downloading dependency: $ktpackScriptJarUrl" }
            val response = context.http
                .prepareGet(ktpackScriptJarUrl)
                .downloadInto(ktpackScriptJarPath)
            if (!response.status.isSuccess()) {
                ktpackScriptJarPath.delete()
                error("Failed to download dependency: ${response.status} $ktpackScriptJarUrl")
            }
        }
    }
    logger.d { "Ktpack Script jar written in ${duration}s" }
}
