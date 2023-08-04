import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.support.zipTo
import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Locale.*
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

val mainGenSrcPath = "build/ktgen-main"
val testGenSrcPath = "build/ktgen-test"

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

val buildTestConstants by tasks.creating {
    val constantsFile = file("${testGenSrcPath}/testConstants.kt")
    onlyIf { !constantsFile.exists() }
    doFirst {
        file(testGenSrcPath).mkdirs()
        constantsFile.writeText(
            """|package ktpack
               |import ktfio.File
               |
               |val buildDir = File("${buildDir.absolutePath.replace("\\", "\\\\")}")
               |val sampleDir = File("${rootProject.file("samples").absolutePath.replace("\\", "\\\\")}")
               |""".trimMargin()
        )
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
        val arch = if (hostOs.isMacOsX) {
            when (konanTarget.architecture) {
                Architecture.ARM64 -> "Arm64"
                else -> "X64"
            }
        } else "" // Other hosts only support one arch, meaning it is omitted from the gradle task name
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

evaluationDependsOn(":ktpack-script")

kotlin {

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.all {
            //freeCompilerArgs += "-Xincremental"
        }
    }
    val nativeTargets = listOfNotNull(
        if (hostOs.isMacOsX) macosX64() else null,
        if (hostOs.isMacOsX) macosArm64() else null,
        if (!hostOs.isWindows) linuxX64() else null,
        if (!hostOs.isLinux) mingwX64("windowsX64") else null,
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
            compileKotlinTask.dependsOn(buildTestConstants)
        }

        binaries {
            executable {
                entryPoint = "ktpack.main"
            }
            all {
                val libType = buildType.name.lowercase()
                val libTarget = target.name.removeSuffix("X64").removeSuffix("Arm64")
                val libLinks = compilation.cinterops.flatMap { settings ->
                    val lib = settings.name
                    val fileName = if (hostOs.isWindows) "${lib}.lib" else "lib${lib}.a"
                    val archPath = if (hostOs.isMacOsX) {
                        when (konanTarget.architecture) {
                            Architecture.ARM64 -> "Arm64/"
                            else -> "X64/"
                        }
                    } else "" // Other hosts only support one arch, meaning it is omitted from the output path
                    listOf(
                        "-include-binary",
                        rootProject.file("libs/$lib/build/lib/main/$libType/$libTarget/${archPath}$fileName").absolutePath
                    )
                }
                compilation.apply {
                    compileKotlinTask.dependsOn(project(":ktpack-script").tasks.findByName("shadowJar"))
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
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
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
                implementation(libs.semver)
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

        if (!hostOs.isLinux) {
            val windowsX64Main by getting {
                dependencies {
                    implementation(libs.ktor.client.winhttp)
                }
            }
        }

        if (!hostOs.isWindows) {
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

fun createPackageReleaseTask(target: String) {
    val extension = if (hostOs.isWindows) ".exe" else ".kexe"
    tasks.create("packageRelease${target.capitalized()}") {
        dependsOn("linkReleaseExecutable${target.capitalized()}X64")
        if (hostOs.isMacOsX) {
            dependsOn("linkReleaseExecutable${target.capitalized()}Arm64")
        }
        doFirst {
            var executable = buildDir.resolve("bin/${target}X64/releaseExecutable/ktpack$extension")
            if (hostOs.isMacOsX) {
                val executableArm = buildDir.resolve("bin/${target}Arm64/releaseExecutable/ktpack$extension")
                val executableUniversal = buildDir.resolve("bin/${target}/releaseExecutable/ktpack$extension")
                executableUniversal.parentFile.mkdirs()
                exec {
                    commandLine("lipo")
                    args(
                        "-create",
                        "-output",
                        executableUniversal.absolutePath,
                        executable.absolutePath,
                        executableArm.absolutePath
                    )
                }.assertNormalExitValue()
                executable = executableUniversal
            }

            val releaseName = "ktpack-$target.zip"
            val releaseBinDir = buildDir.resolve("release/bin")
            val releaseZip = buildDir.resolve("release/$releaseName")
            val releaseZipChecksum = buildDir.resolve("release/$releaseName.sha256")
            copy {
                from(executable)
                into(releaseBinDir)
                rename { if (hostOs.isWindows) it else it.removeSuffix(extension) }
            }
            zipTo(releaseZip, releaseBinDir)
            val sha256 = MessageDigest.getInstance("SHA-256")
            releaseZip.forEachBlock { buffer, _ -> sha256.update(buffer) }
            releaseZipChecksum.writeText(sha256.digest().toHexString())
        }
    }
}

when {
    hostOs.isLinux -> createPackageReleaseTask("linux")
    hostOs.isWindows -> createPackageReleaseTask("windows")
    hostOs.isMacOsX -> createPackageReleaseTask("macos")
}

spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
            //.setUseExperimental(true)
            .editorConfigOverride(
                mapOf(
                    "disabled_rules" to "no-wildcard-imports,no-unused-imports,trailing-comma,filename"
                )
            )
    }
}
