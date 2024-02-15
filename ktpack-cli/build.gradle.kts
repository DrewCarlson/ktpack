import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.support.zipTo
import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.security.MessageDigest

plugins {
    id("internal-lib")
    alias(libs.plugins.serialization)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

evaluationDependsOn(":ktpack-script")

kotlin {
    configure(targets) {
        compilations.named("main") {
            compileTaskProvider.configure {
                dependsOn(project(":ktpack-script").tasks.findByName("shadowJar"))
            }
        }

        if (this is KotlinNativeTarget) {
            binaries {
                executable {
                    baseName = "ktpack"
                    entryPoint("ktpack.main")
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ktpack-internal:compression"))
                implementation(project(":ktpack-internal:core"))
                implementation(project(":ktpack-internal:dependency-resolver"))
                implementation(project(":ktpack-internal:dokka"))
                implementation(project(":ktpack-internal:git"))
                implementation(project(":ktpack-internal:gradle"))
                implementation(project(":ktpack-internal:maven"))
                implementation(project(":ktpack-internal:mongoose"))
                implementation(project(":ktpack-internal:models"))
                implementation(libs.ktfio)
                implementation(libs.ksubprocess)
                implementation(libs.mordant)
                implementation(libs.clikt)
                implementation(libs.cryptohash)
                implementation(libs.xmlutil.serialization)
                implementation(libs.ktoml.core)
                implementation(libs.okio)
                implementation(libs.semver)
                implementation(libs.coroutines.core)
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization)
                implementation(libs.kotlinx.datetime)
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
                        executableArm.absolutePath,
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
