import org.gradle.nativeplatform.platform.internal.*
import java.time.Clock
import java.time.OffsetDateTime
import java.io.ByteArrayOutputStream

plugins {
    id("internal-lib")
    alias(libs.plugins.serialization)
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
               |""".trimMargin(),
        )
    }
}

val buildRuntimeBundle by tasks.creating {
    val debug = (version as String).endsWith("-SNAPSHOT")
    val bundledFile = file("${mainGenSrcPath}/manifest.kt")
    onlyIf { !bundledFile.exists() || !debug }
    doFirst {
        file(mainGenSrcPath).mkdirs()
        /*val jar = rootProject.file("ktpack-script/build/libs/ktpack-script.jar")
        val pathValue = if (debug) {
            """"${jar.absolutePath.replace("\\", "\\\\")}""""
        } else {
            """USER_HOME, ".ktpack", "package-builder", "ktpack-script-${version}.jar""""
        }
        bundledFile.writeText(
            """|package ktpack
               |import kotlinx.io.files.Path
               |import ktpack.util.USER_HOME
               |
               |const val ktpackScriptJarUrl = "https://github.com/DrewCarlson/ktpack/releases/download/v${version}/ktpack-script.jar"
               |val ktpackScriptJarPath = Path($pathValue)
               |""".trimMargin(),
        )*/
    }
}

kotlin {
    configure(targets) {
        compilations.named("main") {
            compileTaskProvider.configure {
                dependsOn(
                    buildRuntimeConstants,
                    buildRuntimeBundle,
                )
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(mainGenSrcPath)
            dependencies {
                api(libs.xmlutil.serialization)
                api(project(":ktpack-internal:platform"))
                implementation(project(":ktpack-internal:compression"))
                implementation(project(":ktpack-internal:git"))
                implementation(project(":ktpack-internal:dokka"))
                implementation(project(":ktpack-internal:models"))
                implementation(libs.kotlin.io)
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
                //implementation(libs.kotlinx.datetime)
                api(libs.tomlkt)
            }
        }

        commonTest {
            dependencies {
                implementation(project(":ktpack-internal:test-utils"))
                implementation(libs.coroutines.test)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }

        windowsMain {
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }

        linuxMain {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }

        appleMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
