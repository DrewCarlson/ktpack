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
        )
    )
    binaries.configureEach {
        compileTask.get().apply {
            includes(rootProject.file("external/mongoose"))
            source(rootProject.file("external/mongoose/mongoose.c"))
            compilerArgs.addAll("-x", "c")
            if (hostOs.isLinux) {
                compilerArgs.add("-std=gnu99")
            } else {
                compilerArgs.add("-std=c99")
            }
        }
    }
}

fun Task.assembleMacos(type: String, target: String) {
    val clangTarget = when (target) {
        "arm64" -> "arm64-apple-macos11"
        else -> "x86_64-apple-macos10.12"
    }
    val outPath = "build/lib/main/${type}/macos/$target"
    val mongooseC = rootProject.file("external/mongoose/mongoose.c")
    doFirst {
        val objBuildFile = file("build/obj/main/${type}/macos/arm64/mongoose.o")
        val libBuildFile = file("$outPath/libmongoose.a")
        objBuildFile.delete()
        libBuildFile.delete()
        objBuildFile.parentFile.mkdirs()
        libBuildFile.parentFile.mkdirs()
        exec {
            workingDir(rootProject.file("external/mongoose"))
            commandLine(
                "clang",
                "-c",
                mongooseC.absolutePath,
                "-target",
                clangTarget,
                "-dM",
                "-m64",
                "-std=c99",
                "-o",
                objBuildFile.absolutePath,
            )
            if (type == "debug") {
                environment("DEBUG", "1")
            }
        }.assertNormalExitValue()
        exec {
            workingDir(file(outPath))
            commandLine("ar", "-rcs", libBuildFile.absolutePath, objBuildFile.absolutePath)
        }.assertNormalExitValue()
    }
}

if (hostOs.isMacOsX) {
    tasks.register("assembleDebugMacosX64") { assembleMacos("debug", "x64") }
    tasks.register("assembleReleaseMacosX64") { assembleMacos("release", "x64") }
    tasks.register("assembleDebugMacosArm64") { assembleMacos("debug", "arm64") }
    tasks.register("assembleReleaseMacosArm64") { assembleMacos("release", "arm64") }
}
