import org.gradle.nativeplatform.platform.internal.*

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
}

val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()

val mainGenSrcPath = "build/ktgen-main"

val buildTestConstants by tasks.creating {
    val constantsFile = file("${mainGenSrcPath}/testConstants.kt")
    onlyIf { !constantsFile.exists() }
    doFirst {
        file(mainGenSrcPath).mkdirs()
        constantsFile.writeText(
            """|package ktpack
               |import okio.Path.Companion.toPath
               |
               |val buildDir = "${rootProject.buildDir.absolutePath.replace("\\", "\\\\")}".toPath()
               |val sampleDir = "${rootProject.file("samples").absolutePath.replace("\\", "\\\\")}".toPath()
               |""".trimMargin()
        )
    }
}

evaluationDependsOn(":ktpack-script")

kotlin {

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.all {
            //freeCompilerArgs += "-Xincremental"
        }
    }
    val nativeTargets = listOfNotNull(
        if (hostOs.isMacOsX) macosX64() else null,
        if (hostOs.isMacOsX) macosArm64() else null,
        if (!hostOs.isWindows) linuxX64() else null,
        if (!hostOs.isLinux) mingwX64("windowsX64") else null,
    )

    configure(nativeTargets) {
        compilations.named("main") {
            kotlinOptions {
                freeCompilerArgs = listOf("-Xallocator=mimalloc")
            }
            compileTaskProvider.configure {
                dependsOn(buildTestConstants)
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.coroutines.FlowPreview")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
            }
        }

        val commonMain by getting {
            kotlin.srcDir(mainGenSrcPath)
            dependencies {
                implementation(project(":ktpack-models"))
                implementation(libs.coroutines.core)
                implementation(libs.okio)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.coroutines.test)
            }
        }

        if (!hostOs.isLinux) {
            val windowsX64Main by getting {
                dependencies {
                }
            }
        }

        if (!hostOs.isWindows) {
            val posixMain by creating {
                dependsOn(commonMain)
            }

            val linuxX64Main by getting {
                dependsOn(posixMain)
                dependencies {
                }
            }
            if (!hostOs.isLinux/* i.e. isMacos */) {
                val darwinMain by creating {
                    dependsOn(posixMain)
                    dependencies {
                    }
                }
                val darwinTest by creating { dependsOn(commonTest) }
                val macosX64Main by getting { dependsOn(darwinMain) }
                val macosX64Test by getting { dependsOn(darwinTest) }
                val macosArm64Main by getting { dependsOn(darwinMain) }
                val macosArm64Test by getting { dependsOn(darwinTest) }
            }
        }
    }
}

spotless {
    kotlin {
        target("src/**/**.kt")
        ktlint(libs.versions.ktlint.get())
    }
}
