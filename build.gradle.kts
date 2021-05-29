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
    maven(url = "https://maven.pkg.jetbrains.space/public/p/ktor/eap")
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
        //if (hostOs.isWindows) mingwX64() else null,
    )
    configure(nativeTargets) {
        compilations.named("main") {
            cinterops {
                if (name.startsWith("linux")) {
                    create("subprocess") {
                        defFile("src/nativeMain/cinterop/subprocess.def")
                    }
                }

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
                    val libPath = file("build/lib/main/${libType}/${libTarget}/libtomlc99.a").absolutePath
                    freeCompilerArgs = freeCompilerArgs + listOf("-include-binary", libPath)
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
            }
        }

        val targets = nativeTargets.map(KotlinTarget::getName)
        configure(targets.map { named("${it}Main").get() }) {
            kotlin.srcDirs("src/nativeMain/kotlin")
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
                implementation(libs.clikt)
                implementation(libs.ktor.client.core)
                // TODO: Windows support is going to be annoying with curl...
                //implementation(libs.ktor.client.curl)
            }
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
