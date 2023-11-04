import org.gradle.nativeplatform.platform.internal.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family

plugins {
    id("internal-lib")
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

evaluationDependsOn(":libs:mongoose")

afterEvaluate {
    tasks.withType<CInteropProcess> {
        val osName = when (konanTarget.family) {
            Family.OSX -> "Macos"
            Family.MINGW -> "Windows"
            Family.LINUX -> "Linux"
            else -> error("Unsupported build target $konanTarget for $name")
        }
        val arch = if (hostOs.isMacOsX) {
            when (konanTarget.architecture) {
                Architecture.ARM64 -> "Arm64"
                else -> "X64"
            }
        } else "" // Other hosts only support one arch, meaning it is omitted from the gradle task name
        val buildTasks = listOfNotNull(
            tasks.findByPath(":libs:${interopName}:assembleDebug$osName${arch}"),
            tasks.findByPath(":libs:${interopName}:assembleRelease$osName${arch}")
        )
        if (buildTasks.isEmpty()) {
            logger.warn("Native build tasks were not found for '$name' on $konanTarget.")
        }
        dependsOn(buildTasks)
    }
}

kotlin {
    configure(targets.filterIsInstance<KotlinNativeTarget>()) {
        compilations.named("main") {
            cinterops {
                create("mongoose") {
                    includeDirs(rootProject.file("external/mongoose"))
                    defFile("src/nativeMain/cinterop/mongoose.def")
                }
            }

            kotlinOptions {
                val libType = NativeBuildType.RELEASE.name.lowercase()
                val libTarget = target.name.removeSuffix("X64").removeSuffix("Arm64")
                val libLinks = cinterops.flatMap { settings ->
                    val lib = settings.name
                    val fileName = if (hostOs.isWindows) "${lib}.lib" else "lib${lib}.a"
                    val archPath = if (hostOs.isMacOsX) {
                        when (konanTarget.architecture) {
                            Architecture.ARM64 -> "Arm64/"
                            else -> "X64/"
                        }
                    } else "" // Other hosts only support one arch, meaning it is omitted from the output path
                    listOf(
                        "-include-binary",
                        rootProject.file("libs/$lib/build/lib/main/$libType/$libTarget/${archPath}$fileName").absolutePath
                    )
                }

                freeCompilerArgs = freeCompilerArgs + libLinks
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.ktor.client.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.coroutines.test)
            }
        }
    }
}
