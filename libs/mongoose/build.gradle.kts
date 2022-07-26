import org.gradle.nativeplatform.platform.internal.*

plugins {
    `cpp-library`
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

library {
    baseName.set("mongoose")
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
            includes(rootProject.file("external/mongoose"))
            source(rootProject.file("external/mongoose/mongoose.c"))
            compilerArgs.addAll("-x", "c", "-std=c99")
        }
    }
}

fun Task.assembleMacosArm64(type: String) {
    val outPath = "build/lib/main/${type}/macos/arm64"
    inputs.files(
        rootProject.file("external/mongoose/mongoose.c"),
        rootProject.file("external/mongoose/mongoose.h"),
    )
    outputs.files(file("$outPath/libmongoose.a"))
    doFirst {
        val execResult = exec {
            workingDir(rootProject.file("external/mongoose"))
            commandLine("make", "CFLAGS=--target=arm64-apple-macos11", "clean", "all")
            if (type == "debug") {
                environment("DEBUG", "1")
            }
        }
        if (execResult.exitValue == 0) {
            copy {
                from(rootProject.file("external/mongoose"))
                into(file(outPath))
                include("mongoose.a")
                rename { "libmongoose.a" }
            }
        } else {
            error("Failed to build mongoose")
        }
    }
}

if (hostOs.isMacOsX) {
    tasks.register("assembleDebugMacosArm64") { assembleMacosArm64("debug") }
    tasks.register("assembleReleaseMacosArm64") { assembleMacosArm64("release") }
}
