import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.support.zipTo
import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import java.time.Clock
import java.time.OffsetDateTime
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
               |import ktpack.util.pathFrom
               |import ktpack.util.USER_HOME
               |
               |const val ktpackScriptJarUrl = "https://github.com/DrewCarlson/ktpack/releases/download/${version}/ktpack-script.jar"
               |val ktpackScriptJarPath = pathFrom($pathValue)
               |""".trimMargin()
        )
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
            kotlinOptions {
                freeCompilerArgs = listOf("-Xallocator=mimalloc")
            }
            compileTaskProvider.configure {
                dependsOn(
                    buildRuntimeConstants,
                    buildRuntimeBundle,
                )
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
                api(libs.xmlutil.serialization)
                implementation(project(":ktpack-internal:platform"))
                implementation(project(":ktpack-internal:compression"))
                implementation(project(":ktpack-internal:git"))
                implementation(project(":ktpack-models"))
                implementation(libs.ktfio)
                implementation(libs.ksubprocess)
                implementation(libs.mordant)
                implementation(libs.clikt)
                implementation(libs.cryptohash)
                implementation(libs.semver)
                implementation(libs.coroutines.core)
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization)
                implementation(libs.kotlinx.datetime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(project(":ktpack-internal:test-utils"))
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

spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
    }
}
