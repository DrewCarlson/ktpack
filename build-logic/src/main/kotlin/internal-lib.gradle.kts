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
                //freeCompilerArgs = listOf("-Xallocator=mimalloc")
            }
        }
    }

    applyDefaultHierarchyTemplate()

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


        if (hostOs.isWindows) {
            named("windowsX64Main") {
                dependencies {
                }
            }
        }

        if (!hostOs.isWindows) {
            create("posixMain") {
                dependsOn(getByName("nativeMain"))
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
            val appleMain by creating {
                dependsOn(getByName("posixMain"))
                dependencies {
                }
            }
        }
    }
}

/*spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
    }
}*/
