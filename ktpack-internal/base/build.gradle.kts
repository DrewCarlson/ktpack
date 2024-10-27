import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.OffsetDateTime

plugins {
    id("internal-lib")
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

val mainGenSrcPath = "build/ktgen-main"

val buildRuntimeConstants by tasks.creating {
    doFirst {
        file(mainGenSrcPath).mkdirs()
        val git = if (hostOs.isWindows) "git.exe" else "git"
        val sha = ByteArrayOutputStream()
            .also { out ->
                exec {
                    commandLine(git, "rev-parse", "HEAD")
                    standardOutput = out
                }.assertNormalExitValue()
            }.toString(Charsets.UTF_8)
            .trim()
        val isDirty = exec {
            commandLine(git, "diff", "--quiet")
            isIgnoreExitValue = true
        }.exitValue == 1

        buildConstFile("ktpack", "Ktpack") {
            add("VERSION", version.toString())
            add("BUILD_SHA", sha + if (isDirty) "-dirty" else "")
            add("BUILD_DATE", OffsetDateTime.now(Clock.systemUTC()).toString())
            add("KOTLIN_VERSION", libs.versions.kotlin)
            add("KTOR_VERSION", libs.versions.ktorio)
            add("COROUTINES_VERSION", libs.versions.coroutines)
            add("SERIALIZATION_VERSION", libs.versions.serialization)
            add("DOKKA_VERSION", libs.versions.dokka)
        }.writeTo(file(mainGenSrcPath))
    }
}

/*val buildRuntimeBundle by tasks.creating {
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
}*/

kotlin {
    configure(targets) {
        compilations.named("main") {
            compileTaskProvider.configure {
                dependsOn(
                    buildRuntimeConstants,
                    // buildRuntimeBundle,
                )
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(mainGenSrcPath)
            dependencies {
                api(libs.coroutines.core)
                api(libs.kotlin.io)
                api(libs.ktor.client.core)
                api(libs.serialization.core)
                api(libs.serialization.json)
                api(libs.xmlutil.serialization)
                api(libs.tomlkt)
                api(libs.kermit)
                api(libs.cryptohash)
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
