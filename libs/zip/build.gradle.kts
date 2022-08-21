import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    `cpp-library`
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

library {
    baseName.set("zip")
    linkage.set(listOf(Linkage.STATIC))
    targetMachines.addAll(
        listOfNotNull(
            machines.linux.x86_64,
            machines.windows.x86_64,
            machines.macOS.x86_64,
        )
    )
    binaries.configureEach {
        compileTask.get().apply {
            includes(rootProject.file("external/zip/src"))
            source(rootProject.file("external/zip/src/zip.c"))
            compilerArgs.addAll("-x", "c")
        }
    }
}

fun Task.assembleMacosArm64(type: String) {
    val typeCapitalized = type.first().toUpperCase() + type.drop(1)
    val outPath = "build/lib/main/${type}/macos/arm64"
    val zipC = rootProject.file("external/zip/zip.c")
    val compileOpts = file("build/tmp/compile${typeCapitalized}Macos/options.txt")
    dependsOn("assemble${typeCapitalized}Macos")
    onlyIf { compileOpts.exists() }
    doFirst {
        val objBuildFile = file("build/obj/main/${type}/macos/arm64/zip.o")
        val libBuildFile = file("$outPath/libzip.a")
        objBuildFile.delete()
        libBuildFile.delete()
        objBuildFile.parentFile.mkdirs()
        libBuildFile.parentFile.mkdirs()
        exec {
            workingDir(rootProject.file("external/zip"))
            commandLine(
                "clang",
                "@${compileOpts.absolutePath}",
                "-m64",
                zipC.absolutePath,
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
