import org.gradle.api.internal.artifacts.transform.UnzipTransform
import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Locale.*
import java.io.ByteArrayOutputStream
import java.net.URL

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
    //alias(libs.plugins.completeKotlin)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

val mainGenSrcPath = "build/ktgen-main"
val testGenSrcPath = "build/ktgen/config"

val installTestConfig by tasks.creating {
    val configFile = file("${testGenSrcPath}/config.kt")
    onlyIf { !configFile.exists() || gradle.startParameter.taskNames.contains("publish") }
    doFirst {
        file(testGenSrcPath).mkdirs()
        if (!configFile.exists()) {
            val extension = if (hostOs.isWindows) "exe" else "kexe"
            val target = when {
                hostOs.isWindows -> "windowsX64"
                hostOs.isLinux -> "linuxX64"
                hostOs.isMacOsX -> if (System.getProperty("os.arch") == "aarch64") "macosArm64" else "macosX64"
                else -> error("Unsupported host operating system")
            }
            configFile.writeText(
                """|package ktpack
                   |
                   |import ktfio.File
                   |import ktfio.nestedFile
                   |
                   |val KTPACK_BIN = "${file("build/bin/${target}/debugExecutable/ktpack.$extension").absolutePath}"
                   |
                   |fun getSample(vararg names: String): File {
                   |    var file = File("${rootProject.file("samples").absolutePath}")
                   |    names.forEach { name -> file = file.nestedFile(name) }
                   |    return file
                   |}
                   |
                   |fun getSamplePath(name: String): String = getSample(name).getAbsolutePath()
                   |""".trimMargin().replace("\\", "\\\\")
            )
        }
    }
}

val buildRuntimeConstants by tasks.creating {
    val constantsFile = file("${mainGenSrcPath}/constants.kt")
    onlyIf { !constantsFile.exists() }
    doFirst {
        file(mainGenSrcPath).mkdirs()
        val git = if (hostOs.isWindows) "git.exe" else "git"
        val sha = ByteArrayOutputStream().also { out ->
            exec {
                commandLine(git, "rev-parse", "HEAD")
                standardOutput = out
            }.assertNormalExitValue()
        }.toString(Charsets.UTF_8).trim()
        val isDirty = exec {
            commandLine(git, "diff", "--quiet")
            isIgnoreExitValue = true
        }.exitValue == 1
        constantsFile.writeText(
            """|package ktpack
               |
               |object Ktpack {
               |    const val VERSION = "$version"
               |    const val BUILD_SHA = "$sha${if (isDirty) "-dirty" else ""}"
               |    const val BUILD_DATE = "${OffsetDateTime.now(Clock.systemUTC())}"
               |    const val KOTLIN_VERSION = "${libs.versions.kotlin.get()}"
               |    const val KTOR_VERSION = "${libs.versions.ktorio.get()}"
               |    const val COROUTINES_VERSION = "${libs.versions.coroutines.get()}"
               |    const val SERIALIZATION_VERSION = "${libs.versions.serialization.get()}"
               |}
               |""".trimMargin()
        )
    }
}

val buildRuntimeBundle by tasks.creating {
    val debug = (version as String).endsWith("-SNAPSHOT")
    val bundledFile = file("${mainGenSrcPath}/manifest.kt")
    onlyIf { !bundledFile.exists() || !debug }
    dependsOn(":ktpack-script:shadowJar")
    doFirst {
        file(mainGenSrcPath).mkdirs()
        val jar = rootProject.file("ktpack-script/build/libs/ktpack-script.jar")
        val pathValue = if (debug) {
            """"${jar.absolutePath.replace("\\", "\\\\")}""""
        } else {
            """USER_HOME, ".ktpack", "package-builder", "ktpack-script-${version}.jar""""
        }
        bundledFile.writeText(
            """|package ktpack
               |import ktfio.File
               |import ktpack.util.USER_HOME
               |
               |const val ktpackScriptJarUrl = "https://github.com/DrewCarlson/ktpack/releases/download/${version}/ktpack-script.jar"
               |val ktpackScriptJarPath by lazy { File($pathValue) }
               |""".trimMargin()
        )
    }
}

val installKotlincForTests by tasks.creating {
    val ktVersion = libs.versions.kotlin.get()
    val compilerFolderName = "kotlin-compiler-prebuilt-$ktVersion"
    val konanDir = File(System.getProperty("user.home"), ".konan")
    val compilerDir = File(konanDir, compilerFolderName)
    onlyIf { !compilerDir.exists() || compilerDir.listFiles().orEmpty().isEmpty() }
    doFirst {
        val downloadFile = File(konanDir, "kotlin-compiler-jvm.zip")
        downloadFile.outputStream().use { out ->
            URL("https://github.com/JetBrains/kotlin/releases/download/v$ktVersion/kotlin-compiler-${ktVersion}.zip")
                .openStream()
                .buffered()
                .use { it.copyTo(out) }
        }

        copy {
            from(zipTree(downloadFile)) {
                // JVM releases are zipped with a single `kotlinc` folder
                includeEmptyDirs = false
                eachFile {
                    val path = relativePath.segments.drop(1).joinToString(File.separator)
                    relativePath = RelativePath(!isDirectory, path)
                }
            }
            into(compilerDir)
        }

        downloadFile.delete()
    }
}

evaluationDependsOn(":libs:mongoose")
evaluationDependsOn(":libs:zip")

afterEvaluate {
    tasks.withType<CInteropProcess> {
        val osName = when (konanTarget.family) {
            Family.OSX -> "Macos"
            Family.MINGW -> "Windows"
            Family.LINUX -> "Linux"
            else -> error("Unsupported build target $konanTarget for $name")
        }
        val arch = when (konanTarget.architecture) {
            Architecture.ARM64 -> "Arm64"
            else -> ""
        }
        val buildTasks = listOfNotNull(
            tasks.findByPath(":libs:${interopName}:assembleDebug$osName${arch}"),
            tasks.findByPath(":libs:${interopName}:assembleRelease$osName${arch}")
        )
        if (buildTasks.isEmpty()) {
            logger.warn("Native build tasks were not found for '$name' on $konanTarget.")
        }
        dependsOn(buildTasks)
    }
}

kotlin {
    val nativeTargets = listOfNotNull(
        if (hostOs.isMacOsX) macosX64() else null,
        if (hostOs.isMacOsX) macosArm64() else null,
        if (hostOs.isMacOsX || hostOs.isLinux) linuxX64() else null,
        mingwX64("windowsX64"),
    )

    configure(nativeTargets) {
        compilations.named("main") {
            cinterops {
                create("mongoose") {
                    includeDirs(rootProject.file("external/mongoose"))
                    defFile("src/commonMain/cinterop/mongoose.def")
                }
                if (hostOs.isWindows) {
                    create("zip") {
                        includeDirs(rootProject.file("external/zip/src"))
                        defFile("src/commonMain/cinterop/zip.def")
                    }
                }
            }

            kotlinOptions {
                freeCompilerArgs = listOf("-Xallocator=mimalloc")
            }
            compileKotlinTask.dependsOn(
                buildRuntimeConstants,
                buildRuntimeBundle,
            )
        }
        compilations.named("test") {
            val osName = when {
                hostOs.isWindows -> "Windows"
                hostOs.isMacOsX -> "Macos"
                else -> "Linux"
            }
            val arch = when (konanTarget.architecture) {
                Architecture.ARM64 -> "Arm64"
                else -> konanTarget.architecture.name
            }
            compileKotlinTask.dependsOn(
                "linkDebugExecutable$osName${arch}",
                installTestConfig,
                installKotlincForTests,
            )
        }

        binaries {
            executable {
                entryPoint = "ktpack.main"
            }
            all {
                val libType = buildType.name.toLowerCase(ROOT)
                val libTarget = target.name.removeSuffix("X64").removeSuffix("Arm64")
                val arch = when (konanTarget.architecture) {
                    Architecture.X64 -> "x86-64"
                    Architecture.X86 -> "x86"
                    Architecture.ARM64 -> "arm64"
                    else -> error("Unsupported host operating system")
                }
                val libLinks = compilation.cinterops.flatMap { settings ->
                    val lib = settings.name
                    val fileName = if (hostOs.isWindows) "${lib}.lib" else "lib${lib}.a"
                    val file = if (arch == "arm64") {
                        rootProject.file("libs/$lib/build/lib/main/$libType/$libTarget/$arch/$fileName")
                    } else {
                        rootProject.file("libs/$lib/build/lib/main/$libType/$libTarget/$fileName")
                    }
                    listOf("-include-binary", file.absolutePath)
                }
                compilation.apply {
                    kotlinOptions {
                        freeCompilerArgs = freeCompilerArgs + libLinks
                    }
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlinx.coroutines.FlowPreview")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }

        val commonMain by getting {
            kotlin.srcDir(mainGenSrcPath)
            dependencies {
                implementation(project(":ktpack-models"))
                implementation(libs.ktfio)
                implementation(libs.ksubprocess)
                implementation(libs.mordant)
                implementation(libs.clikt)
                implementation(libs.cryptohash)
                implementation(libs.xmlutil.serialization)
                implementation(libs.coroutines.core)
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization)
            }
        }

        val commonTest by getting {
            kotlin.srcDir(testGenSrcPath)
            dependencies {
                implementation(libs.coroutines.test)
            }
        }

        val windowsX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }

        if (hostOs.isLinux || hostOs.isMacOsX) {
            val posixMain by creating {
                dependsOn(commonMain)
            }

            val linuxX64Main by getting {
                dependsOn(posixMain)
                dependencies {
                    implementation(libs.ktor.client.curl)
                }
            }
            if (!hostOs.isLinux/* i.e. isMacos */) {
                val darwinMain by creating {
                    dependsOn(posixMain)
                    dependencies {
                        implementation(libs.ktor.client.darwin)
                    }
                }
                val darwinTest by creating { dependsOn(commonTest) }
                val macosX64Main by getting { dependsOn(darwinMain) }
                val macosX64Test by getting { dependsOn(darwinTest) }
                val macosArm64Main by getting { dependsOn(darwinMain) }
                val macosArm64Test by getting { dependsOn(darwinTest) }
            }
        }
    }
}

spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
            .setUseExperimental(true)
            .editorConfigOverride(
                mapOf(
                    "disabled_rules" to "no-wildcard-imports,no-unused-imports,trailing-comma,filename"
                )
            )
    }
}
