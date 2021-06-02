import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.util.Locale.*

plugins {
    kotlin("multiplatform") version "1.5.10"
    kotlin("plugin.serialization") version "1.5.10"
    `cpp-library`
}

repositories {
    mavenCentral()
}

kotlin {
    // TODO: Right now the build is structured with the basic
    //   single host target approach, where only the host os
    //   is configured and a common native source folder is shared.
    //   This is ideal until c-interop commonization is available
    //   and perhaps longer into the future as (macos/linux)+windows
    //   commonization does not work.
    //   (1.5.20-M1+ works here if windows is disabled, but bigger fish..)
    //   With that said, the buildscripts are written to configure all
    //   native targets at the same time so no major changes are required.
    val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()
    val nativeTargets = listOfNotNull(
        if (hostOs.isMacOsX) macosX64() else null,
        if (hostOs.isLinux) linuxX64() else null,
        if (hostOs.isWindows) mingwX64("windowsX64") else null,
    )
    configure(nativeTargets) {
        compilations.named("main") {
            cinterops {
                create("tomlc99") {
                    includeDirs("external/tomlc99")
                    defFile("src/nativeMain/cinterop/tomlc99.def")
                    val tomlTaskName = this@configure.name.capitalize(ROOT).removeSuffix("X64")
                    tasks.getByName(interopProcessingTaskName) {
                        dependsOn("assembleDebug$tomlTaskName", "assembleRelease$tomlTaskName")
                    }
                }
            }

            kotlinOptions {
                freeCompilerArgs = listOf("-Xallocator=mimalloc")
            }
        }

        binaries {
            all {
                compilation.kotlinOptions {
                    val libType = buildType.name.toLowerCase(ROOT)
                    val libTarget = target.name.removeSuffix("X64")
                    val libRoot = "build/lib/main/${libType}/${libTarget}"
                    val libName = if (hostOs.isWindows) "tomlc99.lib" else "libtomlc99.a"
                    freeCompilerArgs = freeCompilerArgs + listOf(
                        "-include-binary",
                        file("$libRoot/$libName").absolutePath
                    )
                }
            }
            executable {
                entryPoint = "ktpack.main"
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
                useExperimentalAnnotation("kotlin.time.ExperimentalTime")
            }
        }

        named("commonMain") {
            dependencies {
                implementation("me.archinamon:fileio")
                implementation("com.github.ajalt.mordant:mordant")
                implementation("com.github.xfel.ksubprocess:ksubprocess")
                implementation(libs.coroutines.core)
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
                implementation(libs.clikt)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.curl)
                implementation(libs.ktor.client.serialization)
            }
        }

        val targets = nativeTargets.map(KotlinTarget::getName)
        configure(targets.map { named("${it}Main").get() }) {
            kotlin.srcDirs("src/nativeMain/kotlin")
        }
    }
}

library {
    baseName.set("tomlc99")
    linkage.set(listOf(Linkage.STATIC))
    targetMachines.addAll(
        machines.linux.x86_64,
        machines.windows.x86_64,
        machines.macOS.x86_64,
    )
    binaries.configureEach {
        compileTask.get().apply {
            includes("external/tomlc99")
            source("external/tomlc99/toml.c")
            compilerArgs.addAll("-x", "c", "-std=c99")
        }
    }
}
