import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import java.util.Locale.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
    //alias(libs.plugins.completeKotlin)
    `cpp-library`
}

yarn.lockFileDirectory = file("gradle/kotlin-js-store")

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

library {
    baseName.set("tomlc99")
    linkage.set(listOf(Linkage.STATIC))
    targetMachines.addAll(
        listOfNotNull(
            machines.linux.x86_64,
            machines.windows.x86_64,
            machines.windows.x86,
            machines.macOS.x86_64,
        )
    )
    binaries.configureEach {
        compileTask.get().apply {
            includes("external/tomlc99")
            source("external/tomlc99/toml.c")
            compilerArgs.addAll("-x", "c", "-std=c99")
        }
    }
}

fun Task.assembleMacosArm64(type: String) {
    val outPath = "build/lib/main/${type}/macos/arm64"
    inputs.files("external/tomlc99/toml.c", "external/tomlc99/toml.h")
    outputs.files("$outPath/libtomlc99.a")
    doFirst {
        val execResult = exec {
            workingDir("external/tomlc99")
            commandLine("make", "CFLAGS=--target=arm64-apple-macos11", "clean", "all")
            if (type == "debug") {
                environment("DEBUG", "1")
            }
        }
        if (execResult.exitValue == 0) {
            copy {
                from("external/tomlc99")
                into(outPath)
                include("libtoml.a")
                rename { "libtomlc99.a" }
            }
        } else {
            error("Failed to build tomlc99")
        }
    }
}

if (hostOs.isMacOsX) {
    tasks.create("assembleDebugMacosArm64") { assembleMacosArm64("debug") }
    tasks.create("assembleReleaseMacosArm64") { assembleMacosArm64("release") }
}

val testGenSrcPath = "build/ktgen/config"

val installTestConfig by tasks.creating {
    val configFile = rootProject.file("${testGenSrcPath}/config.kt")
    onlyIf { !configFile.exists() }
    doFirst {
        rootProject.file(testGenSrcPath).also { if (!it.exists()) it.mkdirs() }
        if (!configFile.exists()) {
            val os = org.gradle.internal.os.OperatingSystem.current()
            val extension = when {
                os.isWindows -> "exe"
                else -> "kexe"
            }
            val target = when {
                os.isWindows -> "windowsX64"
                os.isLinux -> "linuxX64"
                os.isMacOsX -> if (System.getProperty("os.arch") == "aarch64") {
                    "macosArm64"
                } else {
                    "macosX64"
                }

                else -> error("Unsupported host operating system")
            }
            configFile.writeText(
                """
                package ktpack
                import ktfio.File
                val KTPACK = File("${buildDir.resolve("bin/${target}/debugExecutable/ktpack.$extension").absolutePath}")
                fun getSample(vararg name: String): File {
                    return File(
                        (listOf("${file("samples").absolutePath}") + name)
                            .joinToString("${File.separator}")
                    )
                }
                fun getSamplePath(name: String): String = getSample(name).getAbsolutePath()
                """.trimIndent().replace("\\", "\\\\")
            )
        }
    }
}

afterEvaluate {
    tasks.withType<CInteropProcess> {
        val tomlTaskName = when (konanTarget.family) {
            Family.OSX -> "Macos"
            Family.MINGW -> "Windows"
            Family.LINUX -> "Linux"
            else -> error("Unsupported build target $konanTarget for $name")
        }
        val arch = when (konanTarget.architecture) {
            Architecture.ARM64 -> "Arm64"
            Architecture.X64 -> "X86-64"
            else -> konanTarget.architecture.name
        }
        val buildTasks = listOfNotNull(
            tasks.findByName("assembleDebug$tomlTaskName${arch}"),
            tasks.findByName("assembleRelease$tomlTaskName${arch}")
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
        linuxX64(),
        mingwX64("windowsX64"),
    )

    configure(nativeTargets) {
        compilations.named("main") {
            cinterops {
                create("tomlc99") {
                    includeDirs("external/tomlc99")
                    defFile("src/commonMain/cinterop/tomlc99.def")
                }
            }

            kotlinOptions {
                freeCompilerArgs = listOf("-Xallocator=mimalloc")
            }
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
            compileKotlinTask.dependsOn("linkDebugExecutable$osName${arch}", installTestConfig)
        }

        binaries {
            all {
                compilation.kotlinOptions {
                    val libType = buildType.name.toLowerCase(ROOT)
                    val libTarget = target.name.removeSuffix("X64").removeSuffix("Arm64")
                    val arch = when (konanTarget.architecture) {
                        Architecture.X64 -> "x86-64"
                        Architecture.X86 -> "x86"
                        Architecture.ARM64 -> "arm64"
                        else -> error("Unsupported host operating system")
                    }
                    val libRoot = "build/lib/main/$libType/$libTarget/$arch"
                    val libName = if (hostOs.isWindows) "tomlc99.lib" else "libtomlc99.a"
                    freeCompilerArgs = freeCompilerArgs + listOf(
                        "-include-binary", file("$libRoot/$libName").absolutePath,
                    )
                    if (hostOs.isWindows) {
                        val base = System.getenv("RUNNER_TEMP").orEmpty().ifEmpty { "C:" }
                        linkerOpts("-L$base\\msys64\\mingw64\\lib")
                        freeCompilerArgs = freeCompilerArgs + listOf(
                            "-include-binary", "$base\\msys64\\mingw64\\lib\\libcurl.dll.a",
                        )
                    }
                }
            }
            executable {
                entryPoint = "ktpack.main"
            }
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }

        val commonMain by getting {
            dependencies {
                implementation(libs.ktfio)
                implementation(libs.ksubprocess)
                implementation(libs.mordant)
                implementation(libs.clikt)
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
                implementation(libs.ktor.client.curl)
            }
        }

        val posixMain by creating {
            dependsOn(commonMain)
        }

        val linuxX64Main by getting {
            dependsOn(posixMain)
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }

        if (hostOs.isMacOsX) {
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
