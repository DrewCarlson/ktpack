import org.gradle.nativeplatform.platform.internal.*

plugins {
    kotlin("multiplatform")
    id("io.kotest.multiplatform")
    //alias(libs.plugins.spotless)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

kotlin {
    jvmToolchain(17)
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
                freeCompilerArgs = listOf("-Xexpect-actual-classes")
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        all {
            languageSettings {
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlinx.coroutines.FlowPreview")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
            }
        }

        named("commonMain") {
            dependencies {
                api(libs.findLibrary("kermit").get())
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.findLibrary("kotest-assertions").get())
                implementation(libs.findLibrary("kotest-property").get())
                implementation(libs.findLibrary("kotest-api").get())
            }
        }

        nativeTest {
            dependencies {
                implementation(libs.findLibrary("kotest-native").get())
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.findLibrary("kotest-junit5").get())
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
            named("appleMain") {
                dependsOn(getByName("posixMain"))
                dependencies {
                }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

/*spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
    }
}*/
