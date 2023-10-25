import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.support.zipTo
import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.daemon.common.toHexString
import java.security.MessageDigest

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

evaluationDependsOn(":ktpack-script")

kotlin {
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
                dependsOn(project(":ktpack-script").tasks.findByName("shadowJar"))
            }
        }

        binaries {
            executable {
                baseName = "ktpack"
                entryPoint("ktpack.main")
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
            dependencies {
                implementation(project(":ktpack-internal:compression"))
                implementation(project(":ktpack-internal:core"))
                implementation(project(":ktpack-internal:dependency-resolver"))
                implementation(project(":ktpack-internal:dokka"))
                implementation(project(":ktpack-internal:git"))
                implementation(project(":ktpack-internal:gradle"))
                implementation(project(":ktpack-internal:maven"))
                implementation(project(":ktpack-internal:mongoose"))
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
    }
}
