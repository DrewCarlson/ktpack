import org.gradle.nativeplatform.platform.internal.*

plugins {
    kotlin("multiplatform")
    //alias(libs.plugins.spotless)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

kotlin {
    val nativeTargets = listOfNotNull(
        if (hostOs.isMacOsX) macosX64() else null,
        if (hostOs.isMacOsX) macosArm64() else null,
        if (hostOs.isLinux) linuxX64() else null,
        if (hostOs.isWindows) mingwX64("windowsX64") else null,
    )

    configure(nativeTargets) {
        compilations.named("main") {
            kotlinOptions {
                freeCompilerArgs = listOf("-Xallocator=mimalloc")
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

        commonMain {
            dependencies {
            }
        }

        commonTest {
            dependencies {
            }
        }

        if (hostOs.isWindows) {
            val windowsX64Main by getting {
                dependencies {
                }
            }
        }


        if (!hostOs.isWindows) {
            val posixMain by creating {
                dependsOn(getByName("commonMain"))
            }
        }

        if (hostOs.isLinux) {
            val linuxX64Main by getting {
                dependsOn(getByName("posixMain"))
                dependencies {
                }
            }
        }

        if (hostOs.isMacOsX) {
            val darwinMain by creating {
                dependsOn(getByName("posixMain"))
                dependencies {
                }
            }
            val darwinTest by creating { dependsOn(getByName("commonTest")) }
            val macosX64Main by getting { dependsOn(darwinMain) }
            val macosX64Test by getting { dependsOn(darwinTest) }
            val macosArm64Main by getting { dependsOn(darwinMain) }
            val macosArm64Test by getting { dependsOn(darwinTest) }
        }
    }
}

/*spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
    }
}*/
