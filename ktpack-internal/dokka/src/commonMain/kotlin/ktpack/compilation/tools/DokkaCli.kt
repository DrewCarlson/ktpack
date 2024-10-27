package ktpack.compilation.tools

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.invoke
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.serialization.encodeToString
import ksubprocess.exec
import ktpack.compilation.tools.models.DokkaConfiguration
import ktpack.compilation.tools.models.PluginsConfiguration
import ktpack.json
import ktpack.util.*

// https://kotlinlang.org/docs/dokka-cli.html
// https://kotlin.github.io/dokka/1.7.10/user_guide/cli/usage/
class DokkaCli(
    private val dokkaCliFolder: Path,
    private val http: HttpClient,
) {

    private val logger = Logger.forClass<DokkaCli>()

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
        dokkaVersion: String,
    ) {
        require(javaPath.isAbsolute) { "Dokka javaPath must be an absolute directory $javaPath" }
        require(outPath.isAbsolute) { "Dokka outPath must be an absolute directory $outPath" }

        val cli = getCli(dokkaVersion) ?: return // TODO: Handle this case
        val dokkaConfigPath = Path(outPath, "dokka", "config-${dokkaConfiguration.moduleName}.json")
        val pluginClasspath = getDokkaCliDownloadUrls(dokkaVersion).mapNotNull { downloadUrl ->
            if (downloadUrl.contains("dokka-cli")) {
                null
            } else {
                Path(dokkaCliFolder, dokkaVersion, downloadUrl.substringAfterLast('/')).toString()
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
        SystemFileSystem.createDirectories(dokkaConfigPath.parent!!)
        dokkaConfigPath.writeString(json.encodeToString(updatedDokkaConfig))
        try {
            val result = Dispatchers.IO {
                exec {
                    arg(javaPath.toString())
                    arg("-jar")
                    arg(cli.toString())
                    arg(dokkaConfigPath.toString())
                    logger.d { "Launching dokka cli:\n${arguments.joinToString(" ")}" }
                }
            }
            logger.d { result.output }
            logger.d { result.errors }
        } finally {
            dokkaConfigPath.delete()
        }
    }

    fun getCli(version: String): Path? {
        val cliPath = Path(dokkaCliFolder, version, "dokka-cli-$version.jar")
        if (cliPath.exists()) {
            return cliPath
        }
        return null
    }

    suspend fun download(version: String): Boolean {
        if (getCli(version) != null) {
            logger.w { "Dokka v$version is already downloaded, nothing to do." }
            return false
        }
        val dokkaVersionPath = Path(dokkaCliFolder, version)
        SystemFileSystem.createDirectories(dokkaVersionPath)

        val downloadUrls = getDokkaCliDownloadUrls(version)
        val fileNames = downloadUrls.map { it.substringAfterLast('/') }
        val missing = fileNames.mapNotNull { fileName ->
            if (Path(dokkaVersionPath, fileName).exists()) {
                null
            } else {
                downloadUrls.first { it.endsWith(fileName) }
            }
        }
        missing.forEach { downloadUrl ->
            val tempPath = Path(SystemTemporaryDirectory, downloadUrl.substringAfterLast('/'))
            logger.d { "Downloading $downloadUrl into $tempPath" }
            logger.i("Downloading Dokka dependency: ${tempPath.name}")
            val response = http.prepareGet(downloadUrl).downloadInto(tempPath)
            if (response.status.isSuccess()) {
                val outPath = Path(dokkaVersionPath, tempPath.name)
                logger.d { "Download complete moving to $outPath" }
                tempPath.renameTo(outPath)
            } else {
                logger.e { "Failed to download dokka dependency ${response.status} ${response.request.url}" }
                return false
            }
        }
        return true
    }

    private fun getDokkaCliDownloadUrls(version: String): List<String> {
        return listOf(
            "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-html-jvm/0.9.1/kotlinx-html-jvm-0.9.1.jar",
            "https://repo1.maven.org/maven2/org/freemarker/freemarker/2.3.32/freemarker-2.3.32.jar",
            "https://repo1.maven.org/maven2/org/jetbrains/dokka/analysis-kotlin-descriptors/$version/analysis-kotlin-descriptors-$version.jar",
            "https://repo1.maven.org/maven2/org/jetbrains/dokka/dokka-base/$version/dokka-base-$version.jar",
            "https://repo1.maven.org/maven2/org/jetbrains/dokka/dokka-cli/$version/dokka-cli-$version.jar",
        )
    }
}
