package ktpack.compilation.tools

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.invoke
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ksubprocess.exec
import ktpack.compilation.tools.models.DokkaConfiguration
import ktpack.compilation.tools.models.PluginsConfiguration
import ktpack.util.TEMP_PATH
import okio.FileSystem
import okio.Path
import okio.buffer

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

// https://kotlinlang.org/docs/dokka-cli.html
// https://kotlin.github.io/dokka/1.7.10/user_guide/cli/usage/
class DokkaCli(
    private val dokkaCliFolder: Path,
    private val http: HttpClient,
    private val fs: FileSystem,
) {

    private val logger = Logger.withTag(DokkaCli::class.simpleName.orEmpty())
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Run Dokka with the provided configuration.
     *
     * **NOTE:** The dokka output is stored in [DokkaConfiguration.outputDir] and
     * not the [outPath] which is used only for build related files.
     *
     * @param javaPath An absolute path to the `java` binary to launch dokka with.
     * @param outPath An absolute path to the directory will build files are stored.
     * @param dokkaConfiguration The dokka configuration to launch dokka with.
     */
    suspend fun runDokka(
        javaPath: Path,
        outPath: Path,
        dokkaConfiguration: DokkaConfiguration,
    ) {
        require(javaPath.isAbsolute) { "Dokka javaPath must be an absolute directory $javaPath" }
        require(outPath.isAbsolute) { "Dokka outPath must be an absolute directory $outPath" }
        val cli = getDefaultCli() ?: return // TODO: Handle this case
        val dokkaConfigPath = outPath / "config-${dokkaConfiguration.moduleName}.json"
        val pluginClasspath = getDokkaCliDownloadUrls("1.9.10").mapNotNull { downloadUrl ->
            if (downloadUrl.contains("dokka-cli")) {
                null
            } else {
                (dokkaCliFolder / downloadUrl.substringAfterLast('/')).toString()
            }
        }
        val updatedDokkaConfig = dokkaConfiguration.copy(
            pluginsClasspath = pluginClasspath,
            pluginsConfiguration = listOf(
                PluginsConfiguration(
                    fqPluginName = "org.jetbrains.dokka.base.DokkaBase",
                    serializationFormat = "JSON",
                    values = "{}",
                ),
            ),
        )
        logger.d { "Writing dokka config to ${dokkaConfigPath}:\n$updatedDokkaConfig" }
        fs.createDirectories(dokkaConfigPath.parent!!)
        fs.write<Unit>(dokkaConfigPath) {
            writeUtf8(json.encodeToString(updatedDokkaConfig))
        }
        val result = Dispatchers.IO {
            exec {
                arg(javaPath.toString())
                arg("-jar")
                arg(cli.toString())
                arg(dokkaConfigPath.toString())
                logger.d { "Launching dokka cli:\n${arguments.joinToString("\n")}" }
            }
        }

        try {
            logger.d { result.output }
            logger.d { result.errors }
        } finally {
            fs.delete(dokkaConfigPath)
        }
    }

    fun getDefaultCli(): Path? {
        // TODO: get dokka version from config
        return getCli("1.9.10")
    }

    fun getCli(version: String): Path? {
        val cliPath = dokkaCliFolder / "dokka-cli-$version.jar"
        if (fs.exists(cliPath)) {
            return cliPath
        }
        return null
    }

    suspend fun download(version: String): Boolean {
        if (getCli(version) != null) {
            logger.w { "Dokka v$version is already downloaded, nothing to do." }
            return false
        }
        fs.createDirectories(dokkaCliFolder)

        val downloadUrls = getDokkaCliDownloadUrls(version)
        val fileNames = downloadUrls.map { it.substringAfterLast('/') }
        val missing = fileNames.mapNotNull { fileName ->
            if (fs.exists(dokkaCliFolder / fileName)) {
                null
            } else {
                downloadUrls.first { it.endsWith(fileName) }
            }
        }
        missing.forEach { downloadUrl ->
            val tempPath = TEMP_PATH / downloadUrl.substringAfterLast('/')
            logger.d { "Downloading $downloadUrl into $tempPath" }
            logger.i("Downloading Dokka dependency: ${tempPath.name}")
            val response = http.prepareGet(downloadUrl).downloadInto(tempPath)
            if (response.status.isSuccess()) {
                val outPath = dokkaCliFolder / tempPath.name
                fs.atomicMove(tempPath, outPath)
            } else {
                logger.e { "Failed to download dokka dependency ${response.status} ${response.request.url}" }
                return false
            }
        }
        return true
    }

    private fun getDokkaCliDownloadUrls(version: String): List<String> {
        return listOf(
            "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-html-jvm/0.8.0/kotlinx-html-jvm-0.8.0.jar",
            "https://repo1.maven.org/maven2/org/freemarker/freemarker/2.3.31/freemarker-2.3.31.jar",
            "https://repo1.maven.org/maven2/org/jetbrains/dokka/analysis-kotlin-descriptors/$version/analysis-kotlin-descriptors-$version.jar",
            "https://repo1.maven.org/maven2/org/jetbrains/dokka/dokka-base/$version/dokka-base-$version.jar",
            "https://repo1.maven.org/maven2/org/jetbrains/dokka/dokka-cli/$version/dokka-cli-$version.jar",
        )
    }

    private suspend fun HttpStatement.downloadInto(
        outputPath: Path,
        bufferSize: Long = DOWNLOAD_BUFFER_SIZE,
    ): HttpResponse {
        return execute { response ->
            val body = response.bodyAsChannel()
            val sink = fs.appendingSink(outputPath)
            val bufferedSink = sink.buffer()
            try {
                while (!body.isClosedForRead) {
                    val packet = body.readRemaining(bufferSize)
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
    }
}
