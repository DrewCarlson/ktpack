import org.gradle.nativeplatform.platform.internal.*

plugins {
    kotlin("multiplatform")
    //alias(libs.plugins.spotless)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

kotlin {
    jvmToolchain(11)
    jvm()
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

        val commonMain by getting {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                api(libs.findLibrary("kermit").get())
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }

        if (hostOs.isWindows) {
            named("windowsX64Main") {
                dependsOn(nativeMain)
                dependencies {
                }
            }
        }

        if (!hostOs.isWindows) {
            create("posixMain") {
                dependsOn(nativeMain)
                dependsOn(getByName("commonMain"))
            }
        }

        if (hostOs.isLinux) {
            named("linuxX64Main") {
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
