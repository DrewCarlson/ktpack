package ktpack.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import ktpack.CliContext
import ktpack.Ktpack
import ktpack.MANIFEST_FILENAME
import ktpack.configuration.KotlinTarget
import ktpack.manifest.ManifestToml
import ktpack.manifest.ModuleToml
import ktpack.manifest.toml
import ktpack.util.*
import okio.*
import okio.Path.Companion.toPath

private enum class Template { BIN, LIB }

private const val DEFAULT_LICENSE = "MIT"
private const val EMPTY = "(empty)"

class NewCommand : CliktCommand(
    name = "new",
    help = "Create a new package.",
) {

    private val moduleNameRegex = """^[A-Za-z0-9._-]+$""".toRegex()

    private val folder by argument("module_name")
        .help("The folder name to use for the new project.")
        .convert { folder ->
            val folderPath = folder.toPath()
            if (folderPath.isAbsolute) {
                folderPath
            } else {
                workingDirectory / folderPath
            }
        }
        .validate { require(moduleNameRegex.matches(it.name)) }

    private val moduleName by option("--name", "-n")
        .help("Set the resulting module name, defaults to the directory name")
        .defaultLazy { runCatching { folder.name }.getOrDefault("") }
        .validate { require(moduleNameRegex.matches(it)) }

    private val interactive by mutuallyExclusiveOptions(
        option("--interactive", "-i")
            .help("Interactively confirm or override all package properties, ignoring any options other than `--name` and `--bin`/`--lib`")
            .flag(),
        option("--no-interactive", "-noin")
            .help("Disable all interactive prompts, using empty values where no defaults are available")
            .flag()
            .convert { !it },
    ).single()

    private val kotlinVersion by option("--kotlin-version", "-k")
        .help("The Kotlin version to use for this project")
        .default(Ktpack.KOTLIN_VERSION)

    private val projectVersion by option("--version", "-v")
        .help("The initial project version number")
        .default("1.0.0")

    private val template by mutuallyExclusiveOptions(
        option("--bin", "-b", metavar = "")
            .flag()
            .help("Use a binary (application) template")
            .convert { Template.BIN },
        option("--lib", "-l", metavar = "")
            .flag()
            .help("Use a library template")
            .convert { Template.LIB },
    ).single().default(Template.BIN)

    private val targets by option("--target", "-t")
        .help("The supported Target platforms")
        .enum<KotlinTarget>()
        .multiple()

    private val publish by option("--publish", "-p")
        .help("Module publishing will be enabled when this flag is set")
        .flag()

    private val license by option("--license")
        .help("Module publishing will be enabled when this flag is set")
        .default(DEFAULT_LICENSE)

    private val description by option("--description", "-d")
        .help("Module description used for publishing")

    private val homepage by option("--homepage")
        .help("Module homepage URL")

    private val repository by option("--repository")
        .help("Module version control repository location")

    private val vcs by option("--vcs")
        .help("Initialize new VCS repository for the specified version control system.")
        .enum<VcsType>(ignoreCase = true)
        .default(VcsType.GIT)

    private val noVcs by option("--no-vcs")
        .help("Do not initialize a new VCS repository.")
        .flag()

    private val context by requireObject<CliContext>()


    override fun run() = runBlocking {
        checkDirDoesNotExist(folder)
        checkMakeDir(folder)

        val packFile = folder / MANIFEST_FILENAME
        packFile.writeUtf8(toml.encodeToString(generateKtpackConf())) { error ->
            context.term.println("${failed("Failed")} package could not be generated for `$packFile`.")
            context.logError(error)
            exitProcess(1)
        }

        val srcDir = folder / "src"
        val kotlinCommonDir = (srcDir / "common" / "kotlin").mkdirs()
        if (!kotlinCommonDir.exists()) {
            context.term.println("${failed("Failed")} source folder could not be created for `$srcDir`.")
            exitProcess(1)
        }

        when (template) {
            Template.BIN -> kotlinCommonDir.generateSourceFile(context, "main.kt", NEW_BIN_SOURCE)
            Template.LIB -> kotlinCommonDir.generateSourceFile(context, "lib.kt", NEW_LIB_SOURCE)
        }

        if (!noVcs) {
            if (vcs == VcsType.GIT && context.gitCli.hasGit()) {
                context.gitCli.initRepository(folder.toString())
                context.term.println("${info("Initialized")} ${VcsType.GIT} repository")
                val gitignorePath = folder / ".gitignore"
                gitignorePath.writeUtf8(NEW_GITIGNORE_SOURCE) { error ->
                    context.term.println("${failed("Failed")} Could not write .gitignore")
                    context.logError(error)
                }
            }
        }

        context.term.println(
            buildString {
                append(success("Created"))
                when (template) {
                    Template.BIN -> append(" binary (application) ")
                    Template.LIB -> append(" library ")
                }
                append("`$moduleName`")
                append(" package")
            },
        )
    }

    private fun checkDirDoesNotExist(targetDir: Path) {
        if (targetDir.exists() && targetDir.mkdirs().isNotEmpty()) {
            context.term.println("${failed("Failed")} path already exists for `$folder`.")
            exitProcess(1)
        }
    }

    private fun checkMakeDir(targetDir: Path) {
        if (!targetDir.exists() && !targetDir.mkdirs().exists()) {
            context.term.println("${failed("Failed")} path could not be generated for `$folder`.")
            exitProcess(1)
        }
    }

    private fun generateKtpackConf() = ManifestToml(
        module = ModuleToml(
            name = flagOrUserPrompt("Project Name", moduleName) { moduleName },
            kotlinVersion = flagOrUserPrompt("Kotlin Version", Ktpack.KOTLIN_VERSION) { kotlinVersion },
            version = flagOrUserPrompt("Project Version", null) { projectVersion },
            publish = flagOrUserPrompt("Publish library", false) { publish },
            license = flagOrUserPrompt("License", "MIT") { license },
            description = flagOrUserPrompt("Description", null) { description },
            homepage = flagOrUserPrompt("Homepage URL", null) { homepage },
            repository = flagOrUserPrompt("Version Control Repository", null) { repository },
            authors = run {
                val authorDetails = context.gitCli.discoverAuthorDetails()
                val defaultAuthor = buildString {
                    append(authorDetails["name"]?.takeUnless(String::isBlank) ?: "Kotlin Developer")
                    authorDetails["email"]?.takeUnless(String::isBlank)?.let { email ->
                        append(" <")
                        append(email)
                        append(">")
                    }
                }
                listOf(flagOrUserPrompt("Author", defaultAuthor) { defaultAuthor })
            },
            targets = targets.takeUnless { interactive == true } ?: run {
                val targetStrings = KotlinTarget.entries.joinToString { info(it.name.lowercase()) }
                context.term.println("${verbose("Available targets")}: $targetStrings")
                val response = context.term.prompt("Comma separated list of targets (leave blank for all)")
                if (response.isNullOrBlank()) {
                    emptyList()
                } else {
                    response.replace(" ", "")
                        .split(",")
                        .map { KotlinTarget.valueOf(it.uppercase()) }
                }
            },
        ),
    )

    private inline fun <reified T> flagOrUserPrompt(
        message: String,
        default: T,
        crossinline flag: () -> T,
    ): T {
        if (interactive == null || interactive == false) {
            return flag()
        }
        val (defaultValue, actualMessage) = when (T::class) {
            Boolean::class -> if (default as Boolean) {
                "Y" to "$message [Y/n]"
            } else {
                "N" to "$message [y/N]"
            }

            String::class -> (default as? String ?: EMPTY) to message
            else -> default?.toString() to message
        }
        val response =
            checkNotNull(context.term.prompt(actualMessage, defaultValue, showDefault = T::class != Boolean::class))
        return when (T::class) {
            String::class -> response.takeUnless { it.isBlank() || it == EMPTY }
            Boolean::class -> response.equals("y", true)
            else -> error("Unsupported flag type: ${T::class}")
        } as T
    }
}

fun Path.generateSourceFile(context: CliContext, fileName: String, contents: String) {
    val sourceFile = this / fileName
    sourceFile.writeUtf8(contents) { error ->
        context.term.println("${failed("Failed")} source could not be created at `$sourceFile`.")
        context.logError(error)
        exitProcess(1)
    }
}

private val NEW_BIN_SOURCE =
    """|fun main() {
       |  println("Hello, World!")
       |}
       |""".trimMargin()

private val NEW_LIB_SOURCE =
    """|fun sayHello() {
       |  println("Hello, World!")
       |}
       |""".trimMargin()

private val NEW_GITIGNORE_SOURCE =
    """|# Ktpack build directories
       |out/
       |# Intellij IDEA/Android Studio project configuration
       |.idea/
       |# Java profiling
       |*.hprof
       |# macOS platform files
       |.DS_Store
       |# Node.js dependencies
       |node_modules/
       |""".trimMargin()
