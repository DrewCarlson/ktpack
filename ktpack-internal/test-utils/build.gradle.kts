plugins {
    id("internal-lib")
}

val mainGenSrcPath = "build/ktgen-main"

val buildTestConstants by tasks.creating {
    val constantsFile = file("${mainGenSrcPath}/testConstants.kt")
    onlyIf { !constantsFile.exists() }
    doFirst {
        file(mainGenSrcPath).mkdirs()
        constantsFile.writeText(
            """|package ktpack
               |import kotlinx.io.files.Path
               |
               |val buildDir = Path("${rootProject.buildDir.absolutePath.replace("\\", "\\\\")}")
               |val sampleDir = Path("${rootProject.file("samples").absolutePath.replace("\\", "\\\\")}")
               |""".trimMargin()
        )
    }
}

kotlin {
    configure(targets) {
        compilations.named("main") {
            compileTaskProvider.configure {
                dependsOn(buildTestConstants)
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(mainGenSrcPath)
            dependencies {
                implementation(project(":ktpack-internal:models"))
                implementation(libs.coroutines.core)
                implementation(libs.kotlin.io)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.coroutines.test)
            }
        }
    }
}
