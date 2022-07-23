import org.gradle.nativeplatform.platform.internal.*

plugins {
    `cpp-library`
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
            includes(rootProject.file("external/tomlc99"))
            source(rootProject.file("external/tomlc99/toml.c"))
            compilerArgs.addAll("-x", "c", "-std=c99")
        }
    }
}

fun Task.assembleMacosArm64(type: String) {
    val outPath = "build/lib/main/${type}/macos/arm64"
    inputs.files(
        rootProject.file("external/tomlc99/toml.c"),
        rootProject.file("external/tomlc99/toml.h"),
    )
    outputs.files(file("$outPath/libtomlc99.a"))
    doFirst {
        val execResult = exec {
            workingDir(rootProject.file("external/tomlc99"))
            commandLine("make", "CFLAGS=--target=arm64-apple-macos11", "clean", "all")
            if (type == "debug") {
                environment("DEBUG", "1")
            }
        }
        if (execResult.exitValue == 0) {
            copy {
                from(rootProject.file("external/tomlc99"))
                into(file(outPath))
                include("libtoml.a")
                rename { "libtomlc99.a" }
            }
        } else {
            error("Failed to build tomlc99")
        }
    }
}

if (hostOs.isMacOsX) {
    tasks.register("assembleDebugMacosArm64") { assembleMacosArm64("debug") }
    tasks.register("assembleReleaseMacosArm64") { assembleMacosArm64("release") }
}
