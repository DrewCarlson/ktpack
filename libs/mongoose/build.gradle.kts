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
            compilerArgs.addAll("-x", "c")
            if (hostOs.isLinux) {
                compilerArgs.add("-std=gnu99")
            } else {
                compilerArgs.add("-std=c99")
            }
        }
    }
}

fun Task.assembleMacosArm64(type: String) {
    val typeCapitalized = type.first().toUpperCase() + type.drop(1)
    val outPath = "build/lib/main/${type}/macos/arm64"
    val mongooseC = rootProject.file("external/mongoose/mongoose.c")
    val compileOpts = file("build/tmp/compile${typeCapitalized}MacosX86-64Cpp/options.txt")
    dependsOn("assemble${typeCapitalized}MacosX86-64")
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
                "@${compileOpts.absolutePath}",
                "-m64",
                mongooseC.absolutePath,
                "-o",
                objBuildFile.absolutePath,
                "--target=arm64-apple-macos11",
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
    tasks.register("assembleDebugMacosArm64") { assembleMacosArm64("debug") }
    tasks.register("assembleReleaseMacosArm64") { assembleMacosArm64("release") }
}
