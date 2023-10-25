import org.gradle.nativeplatform.platform.internal.*

plugins {
    id("internal-lib")
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
    configure(targets) {
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
    }
}
