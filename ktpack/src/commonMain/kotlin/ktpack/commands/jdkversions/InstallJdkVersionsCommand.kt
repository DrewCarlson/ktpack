package ktpack.commands.jdkversions

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.enum
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import ktfio.File
import ktfio.appendBytes
import ktfio.filePathSeparator
import ktpack.KtpackContext
import ktpack.util.*
import platform.posix.getenv

private const val DOWNLOAD_BUFFER_SIZE = 12_294L

class InstallJdkVersionsCommand : CliktCommand(
    name = "install",
    help = "Install a new Jdk version.",
) {

    private val version by argument(
        help = "The Jdk version to install."
    ).default("11")

    private val distribution by option(
        help = "The Jdk distribution to install."
    ).enum<JdkDistribution>()
        .default(JdkDistribution.Zulu)

    private val path by option(
        help = "The folder path to store the install."
    ).convert { File(it) }
        .default(File("${USER_HOME}$filePathSeparator.jdks"))
        .validate { path ->
            (path.exists() && path.isDirectory()) || path.mkdirs()
        }

    private val context by requireObject<KtpackContext>()

    override fun run() = runBlocking {
        if (!path.exists() && !path.mkdirs()) {
            context.term.println("${failed("Failed")} Path does not exist and could not be created '$path'")
            return@runBlocking
        }
        val existingInstalls = JdkInstalls.discover(path)
        val matchedInstall = existingInstalls.firstOrNull { it.distribution == distribution && it.version == version }
        if (matchedInstall != null) {
            context.term.println("${warn("Warning")} Existing installation found at ${matchedInstall.path}, nothing to do")
            return@runBlocking
        }

        check(distribution == JdkDistribution.Zulu) { "Only zulu is supported at this time." }

        context.term.println("${info("JDKs")} Fetching available $distribution JDK versions")

        val osName = when (Platform.osFamily) {
            OsFamily.MACOSX -> "macosx"
            OsFamily.LINUX -> "linux"
            OsFamily.WINDOWS -> "win"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
        val arch = when (Platform.cpuArchitecture) {
            CpuArchitecture.ARM64 -> "aarch64"
            CpuArchitecture.X64 -> "x64"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }
        val extension = when (Platform.osFamily) {
            OsFamily.WINDOWS -> ".zip"
            OsFamily.LINUX, OsFamily.MACOSX -> ".tar.gz"
            else -> error("Unsupported host platform: ${Platform.osFamily} ${Platform.cpuArchitecture}")
        }

        val packageSuffix = "${osName}_$arch$extension"
        val versionFilter = "ca-jdk${version}" // NOTE: Community Availability filter
        val matchedPackages = context.http.listZuluVersions()
            .mapNotNull { releaseFileName ->
                releaseFileName.takeIf { it.endsWith(packageSuffix) && it.contains(versionFilter) }
            }
            .toList()

        if (matchedPackages.isEmpty()) {
            context.term.println("${failed("Failed")} No packages found matching '$version' and '$packageSuffix'")
        } else {
            context.term.println("${info("JDKs")} Found ${matchedPackages.size} packages matching '$version' and '$packageSuffix'")
        }

        val resolvedPackage = matchedPackages.maxByOrNull { packageName ->
            val jdkVersionString = packageName.substringAfter("jdk").substringBefore('-')
            val (major, minor, patch) = jdkVersionString.split('.').map(String::toInt)
            (major * 100000) + (minor * 1000) + patch // TODO: Sort
        }

        if (resolvedPackage == null) {
            context.term.println("${failed("Failed")} $distribution does not have a version matching '$version'")
            return@runBlocking
        }

        context.term.println("${info("Found")} Latest matching package is '$resolvedPackage'")

        val jdkVersionString = resolvedPackage.substringAfter("jdk").substringBefore('-')
        if (JdkInstalls.findJdk(path, jdkVersionString, distribution) != null) {
            context.term.println("${failed("Failed")} JDK ${info(distribution.name)} ${info(jdkVersionString)} is already installed")
            return@runBlocking
        }

        val downloadUrl = "https://cdn.azul.com/zulu/bin/${resolvedPackage}"
        context.term.println("${success("Downloading")} $downloadUrl")

        val tempRoot = checkNotNull(
            getenv("TEMP")?.toKString()
                ?: getenv("TMPDIR")?.toKString()
                ?: "/tmp".takeIf { Platform.osFamily == OsFamily.LINUX }
        ) { "TEMP, TMPDIR env variables is missing, unable to find temp directory" }
        val tempFile = File("${tempRoot}${filePathSeparator}$resolvedPackage")
        val tempOutFile = File("${tempRoot}${filePathSeparator}${resolvedPackage.replace(extension, "")}")
        if (!tempFile.createNewFile() && !(tempFile.delete() && tempFile.createNewFile())) {
            context.term.println("${failed("Downloading")} Unable to create temp file ${tempFile.getAbsolutePath()}")
            return@runBlocking
        }

        val (status, duration) = measureSeconds {
            context.http.prepareGet(downloadUrl) {
                var lastReportedProgress = 0
                onDownload { bytesSentTotal, contentLength ->
                    val completed = ((bytesSentTotal.toDouble() / contentLength) * 100).toInt()
                    if (completed != lastReportedProgress && completed % 10 == 0) {
                        context.term.println("${info("Downloading")} $completed%")
                        lastReportedProgress = completed
                    }
                }
            }.execute { response ->
                try {
                    val body = response.bodyAsChannel()
                    while (!body.isClosedForRead) {
                        val packet = body.readRemaining(DOWNLOAD_BUFFER_SIZE)
                        while (packet.isNotEmpty) {
                            tempFile.appendBytes(packet.readBytes())
                        }
                    }
                } catch (e: IOException) {
                    context.term.println("${failed("Downloading")} IO error whole downloading '$resolvedPackage'")
                    context.term.println("${failed("Downloading")} ${e.printStackTrace()}")
                }
                response.status
            }
        }

        if (!status.isSuccess()) {
            context.term.println("${failed("Downloading")} Received '$status' while downloading $downloadUrl")
            return@runBlocking
        }
        context.term.println("${success("Downloading")} JDK downloaded to '${tempFile.getAbsolutePath()}' in $duration seconds")
        context.term.println("${info("Extracting")} '${resolvedPackage}'")
        val tempFilePath = tempFile.getAbsolutePath()
        try {
            val compressor = if (Platform.osFamily == OsFamily.WINDOWS) Zip else Tar
            val fileCount = compressor.countFiles(tempFilePath)
            if (context.debug) {
                context.term.println("${info("Extracting")} $fileCount files to go")
            }
            var lastReportedProgress = 0
            compressor.extract(tempFilePath, checkNotNull(tempFile.getParent()))
                .flowOn(Dispatchers.Default)
                .collectIndexed { index, filePath ->
                    if (context.debug) {
                        context.term.println("${info("Extracting")} File #${index + 1} to $filePath")
                    }
                    val completed = ((index.toDouble() / fileCount) * 100).toInt()
                    if (completed != lastReportedProgress && completed % 10 == 0) {
                        context.term.println("${info("Extracting")} $completed%")
                        lastReportedProgress = completed
                    }
                }
        } catch (e: ZipException) {
            context.term.println("${failed("Extracting")} Could not extract '${tempFile.getName()}'")
            context.term.println("${failed("Extracting")} ${e.message}")
            return@runBlocking
        }
        context.term.println("${info("Extracting")} All files extracted")

        val newJdkName = "${distribution.name.lowercase()}-${jdkVersionString}"
        val newJdkFolder = File("${path.getAbsolutePath()}${filePathSeparator}${newJdkName}")
        if (tempOutFile.renameTo(newJdkFolder)) {
            context.term.println(
                buildString {
                    append(success("Success"))
                    append(" JDK ")
                    append(info(distribution.name))
                    append(' ')
                    append(info(jdkVersionString))
                    append(" is now installed to '")
                    append(newJdkFolder.getAbsolutePath())
                    append("'")
                }
            )
        } else {
            context.term.println("${failed("Extracting")} Failed to move folder to '${newJdkFolder.getAbsolutePath()}'")
        }
    }

    private suspend fun HttpClient.listZuluVersions(): Sequence<String> {
        val body = get("https://cdn.azul.com/zulu/bin/").bodyAsText()
        val regex = """<a href="(zulu[A-Za-z0-9._-]+)">""".toRegex()
        return regex.findAll(body).map { it.groupValues[1] }
    }
}
