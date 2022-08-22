import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.konan.target.Architecture
import java.net.URL

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
    //alias(libs.plugins.completeKotlin)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

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
            val ktpackBin =
                project(":ktpack").file("build/bin/${target}/debugExecutable/ktpack.$extension").absolutePath
            configFile.writeText(
                """|package ktpack
                   |
                   |import ktfio.File
                   |import ktfio.nestedFile
                   |
                   |val KTPACK_BIN = "$ktpackBin"
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

val installKotlincForTests by tasks.creating {
    val ktVersion = libs.versions.kotlin.get()
    val compilerFolderName = "kotlin-compiler-prebuilt-$ktVersion"
    val konanDir = File(System.getProperty("user.home"), ".konan")
    val compilerDir = File(konanDir, compilerFolderName)
    onlyIf { !compilerDir.exists() || compilerDir.listFiles().orEmpty().isEmpty() }
    doFirst {
        val downloadFile = File(konanDir, "kotlin-compiler-jvm.zip")
        val downloadUrl =
            URL("https://github.com/JetBrains/kotlin/releases/download/v$ktVersion/kotlin-compiler-${ktVersion}.zip")
        downloadFile.outputStream().use { out ->
            downloadUrl.openStream().use { it.copyTo(out) }
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

kotlin {
    val nativeTargets = listOfNotNull(
        if (hostOs.isMacOsX) macosX64() else null,
        if (hostOs.isMacOsX) macosArm64() else null,
        if (!hostOs.isWindows) linuxX64() else null,
        if (!hostOs.isLinux) mingwX64("windowsX64") else null,
    )

    configure(nativeTargets) {
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
                project(":ktpack").tasks.findByName("linkDebugExecutable$osName${arch}"),
                installTestConfig,
                installKotlincForTests,
            )
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
            dependencies {
                implementation(project(":ktpack"))
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