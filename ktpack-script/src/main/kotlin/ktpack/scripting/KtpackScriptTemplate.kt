package ktpack.scripting

import ktpack.internal.Directories
import ktpack.configuration.Import
import ktpack.configuration.KtpackScriptScope
import ktpack.gradle.catalog.LibsGenerator
import ktpack.gradle.catalog.VersionCatalogTomlParser
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.io.path.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache

private const val COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR = "KTPACK_COMPILED_SCRIPTS_CACHE_DIR"
private const val COMPILED_SCRIPTS_CACHE_DIR_PROPERTY = "ktpack.compiled.scripts.cache.dir"
private const val COMPILED_SCRIPTS_CACHE_VERSION = 1
private const val SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME = "__FILE__"

@KotlinScript(
    displayName = "Ktpack Script Compile",
    filePathPattern = ".*pack.*\\.kts",
    hostConfiguration = KtpackHostConfiguration::class,
    compilationConfiguration = KtpackScriptCompilationConfiguration::class,
    evaluationConfiguration = KtpackEvaluationConfiguration::class,
)
public open class KtpackScriptTemplate(
    @Suppress("unused")
    public val args: Array<String>, // DO NOT REMOVE
) : KtpackScriptScope()

public open class KtpackEvaluationConfiguration(
    body: (Builder.() -> Unit) = {},
) : ScriptEvaluationConfiguration(
    {
        body()
        jvm {
            //baseClassLoader(KtpackScriptTemplate::class.java.classLoader)
            //loadDependencies(true)
        }
    },
)

public open class KtpackScriptCompilationConfiguration(
    body: (Builder.() -> Unit),
    beforeParsing: RefineScriptCompilationConfigurationHandler,
    beforeCompiling: RefineScriptCompilationConfigurationHandler,
) : ScriptCompilationConfiguration(
    {
        baseClass(KtpackScriptScope::class)
        defaultImports(
            Import::class,
            DependsOn::class,
            Repository::class,
        )
        defaultImports(
            "ktpack.configuration.*",
            "ktpack.configuration.KotlinTarget",
            "ktpack.configuration.KotlinTarget.*",
        )
        jvm {
            dependenciesFromClassContext(
                KtpackScriptScope::class,
                //wholeClasspath = true,
                libraries = arrayOf(
                    "ktpack-script",
                    "kotlin-scripting-jvm",
                    "kotlin-stdlib",
                    "kotlin-reflect"
                ),
            )
        }

        refineConfiguration {
            onAnnotations(
                Import::class,
                DependsOn::class,
                Repository::class,
                handler = KtpackScriptConfigurator(),
            )
            beforeParsing { context ->
                beforeParsing(context)/*.onSuccess { newConfig ->
                    ScriptCompilationConfiguration(listOf(newConfig)) {
                        if (generatedLibsFile != null) {
                            importScripts.append(FileScriptSource(generatedLibsFile))
                        }
                    }.asSuccess()
                }*/
            }
            beforeCompiling { context ->
                beforeCompiling(context).onSuccess { newConfig ->
                    // TODO: Resolve libs.version.toml correctly in ide
                    val libsTomlPath = Path("C:\\Users\\drewc\\Workspace\\ktpack\\samples\\6-dependencies\\libs.versions.toml")
                    //val libsTomlPath = Path("libs.versions.toml")
                    println("Checking for libs toml at ${libsTomlPath.absolutePathString()}")
                    val generatedLibsFile = if (libsTomlPath.exists()) {
                        val generatedLibsFile = Files.createTempFile("libs-generated-", ".kt")
                        val libsToml = VersionCatalogTomlParser.parse(libsTomlPath.readText())
                        val code = LibsGenerator.generate(libsToml).toString()
                        generatedLibsFile
                            .apply { writeText(code) }
                            .toFile()
                    } else {
                        null
                    }
                    ScriptCompilationConfiguration(listOf(newConfig)) {
                        if (generatedLibsFile != null) {
                            //importScripts.append(FileScriptSource(generatedLibsFile))
                        }
                    }.asSuccess()
                }
            }
        }
        body()

        // TODO: At this point we don't have any info about the script path,
        //   assume the pack script is in the current working directory.
        //   This is true at least for instances launched by ktpack-cli.
        /*val libsTomlPath = Path("C:\\Users\\drewc\\Workspace\\ktpack\\samples\\6-dependencies\\libs.versions.toml")
        //val libsTomlPath = Path("libs.versions.toml")
        println("Checking for libs toml at ${libsTomlPath.absolutePathString()}")
        if (libsTomlPath.exists()) {
            val generatedLibsFile = Files.createTempFile("libs-generated-", ".kt")
            val libsToml = VersionCatalogTomlParser.parse(libsTomlPath.readText())
            val code = LibsGenerator.generate(libsToml).toString()
            generatedLibsFile.writeText(code)

            //importScripts.append(FileScriptSource(generatedLibsFile.toFile()))
        }*/
    },
)

public class KtpackHostConfiguration : ScriptingHostConfiguration(
    {
        jvm {
            val cacheExtSetting = System.getProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
                ?: System.getenv(COMPILED_SCRIPTS_CACHE_DIR_ENV_VAR)
            val cacheBaseDir = when {
                cacheExtSetting == null -> Directories(System.getProperties(), System.getenv()).cache
                    ?.takeIf { it.exists() && it.isDirectory }
                    ?.let { File(it, "ktpack.compiled.cache").apply { mkdir() } }

                cacheExtSetting.isBlank() -> null
                else -> File(cacheExtSetting)
            }?.takeIf { it.exists() && it.isDirectory }
            if (cacheBaseDir != null) {
                compilationCache(
                    CompiledScriptJarsCache { script, scriptCompilationConfiguration ->
                        File(cacheBaseDir, compiledScriptUniqueName(script, scriptCompilationConfiguration) + ".jar")
                    },
                )
            }
        }
    },
)

private fun compiledScriptUniqueName(
    script: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
): String {
    val digestWrapper = MessageDigest.getInstance("SHA-256")

    fun addToDigest(chunk: String) = with(digestWrapper) {
        val chunkBytes = chunk.toByteArray()
        update(chunkBytes.size.toByteArray())
        update(chunkBytes)
    }

    digestWrapper.update(COMPILED_SCRIPTS_CACHE_VERSION.toByteArray())
    addToDigest(script.text)
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            addToDigest(it.key.name)
            addToDigest(it.value.toString())
        }
    return digestWrapper.digest().toHexString()
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })

private fun Int.toByteArray() = ByteBuffer.allocate(Int.SIZE_BYTES).also { it.putInt(this) }.array()
