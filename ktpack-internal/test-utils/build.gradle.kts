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
                implementation(libs.okio)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.coroutines.test)
            }
        }
    }
}
