package ktpack.commands

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.reset
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.invoke
import kotlinx.coroutines.runBlocking
import ksubprocess.Process
import ksubprocess.ProcessArgumentBuilder
import ktpack.CliContext
import ktpack.compilation.ArtifactResult
import ktpack.compilation.ModuleBuilder
import ktpack.configuration.KotlinTarget
import ktpack.configuration.KtpackConf
import ktpack.util.*

class TestCommand : CliktCommand(
    help = "Compile and run test suites.",
) {

    private val logger = Logger.withTag(TestCommand::class.simpleName.orEmpty())
    private val context by requireObject<CliContext>()

    private val userTargets by option("--target", "-t")
        .help("The target platform to build for.")
        .enum<KotlinTarget> { it.name.lowercase() }
        .multiple(listOf(PlatformUtils.getHostTarget()))

    override fun run(): Unit = runBlocking {
        val packageConf = context.loadKtpackConf()
        val moduleBuilder = ModuleBuilder(packageConf.module, context, workingDirectory)

        userTargets.forEach { target ->
            buildAndRunTests(packageConf, moduleBuilder, target)
        }
    }

    private suspend fun buildAndRunTests(
        packageConf: KtpackConf,
        moduleBuilder: ModuleBuilder,
        target: KotlinTarget,
    ) {
        val name = packageConf.module.name
        val modulePath = moduleBuilder.modulePath
        logger.i("${success("Compiling")} ${bold(target.name)} tests for $name ($modulePath)")
        val result = terminal.loadingIndeterminate {
            moduleBuilder.buildTest(target)
        }
        when (result) {
            is ArtifactResult.Success -> {
                logger.d { result.outputText }
                val duration = result.compilationDuration.toString()
                val modeDetails = "dev [unoptimized + debuginfo] target(s)"
                logger.i("${success("Finished")} $modeDetails in ${bold(white(duration))}s")
                executeTestRun(result, target)
            }

            is ArtifactResult.ProcessError -> {
                logger.i("${failed("Failed")} Compilation process failed with exit code (${result.exitCode})")
                logger.i(result.message.orEmpty())
            }

            is ArtifactResult.NoArtifactFound -> {
                logger.i("${failed("Failed")} no test binary to run")
            }

            is ArtifactResult.NoSourceFiles -> {
                logger.i("${failed("Failed")} no source files")
            }
        }
    }

    private suspend fun executeTestRun(
        result: ArtifactResult.Success,
        target: KotlinTarget,
    ) {
        logger.i("${success("Running")} '${result.artifactPath}'")
        try {
            val (exitCode, duration) = measureSeconds {
                terminal.loadingIndeterminate {
                    runBuildArtifact(target, result.artifactPath, result.dependencyArtifacts)
                }
            }
            val durationString = bold(white(duration.toString()))
            if (exitCode == 0) {
                logger.i("${success("Finished")} Tests completed successfully in ${durationString}s")
            } else {
                logger.i("${failed("Failed")} Tests terminated with code ($exitCode) in ${durationString}s")
            }
        } catch (e: IOException) {
            logger.i("${failed("Failed")} Tests could not be started due to an IO error")
            logger.i(e.message.orEmpty())
            logger.e { e.stackTraceToString() }
        }
    }

    private suspend fun runBuildArtifact(
        target: KotlinTarget,
        artifactPath: String,
        dependencyArtifacts: List<String>,
    ): Int {
        val runProcess = Process {
            when (target) {
                KotlinTarget.JS_NODE,
                KotlinTarget.JS_BROWSER,
                -> error("Unsupported test target: $target")

                KotlinTarget.JVM -> configureJunitTestArgs(dependencyArtifacts, artifactPath)

                /*KotlinTarget.JS_NODE -> {
                    val nodeJs = context.nodejsInstalls.findNodejsExe(context.config.nodejs.version)
                    if (nodeJs == null) {
                        logger.i("${failed("Failed")} Could not find Nodejs installation.")
                        exitProcess(1)
                    }
                    arg(nodeJs.toString())
                    arg(artifactPath)
                }*/

                else -> {
                    // native targets
                    arg(artifactPath)
                }
            }

            logger.d { "Executing test runner:\n${arguments.joinToString("\n")}" }
        }

        val stdoutPrefix = bold(brightBlue("[out] "))
        val stderrPrefix = bold(brightRed("[err] "))
        return Dispatchers.Default {
            merge(
                runProcess.stdoutLines.map { stdoutPrefix + reset(it) },
                runProcess.stderrLines.map { stderrPrefix + reset(it) },
            ).collect { line ->
                context.term.println(line)
            }

            runProcess.waitFor()
        }
    }

    private suspend fun ProcessArgumentBuilder.configureJunitTestArgs(
        dependencyArtifacts: List<String>,
        artifactPath: String,
    ) {
        val jdkInstallation = context.jdkInstalls.getDefaultJdk()
        if (jdkInstallation == null) {
            logger.i("${failed("Failed")} Could not find JDK installation.")
            exitProcess(1)
        }

        // TODO: Move junit download handling and make version configurable
        val junitConsoleJar = KTPACK_ROOT / "junit" / "junit-platform-console-standalone-1.10.0.jar"
        if (!junitConsoleJar.exists()) {
            junitConsoleJar.parent?.mkdirs()
            val response = context.http
                .prepareGet("https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.0/junit-platform-console-standalone-1.10.0.jar")
                .downloadInto(junitConsoleJar)
            if (response.status.isSuccess()) {
                logger.i("Thanks for using JUnit! Support its development at https://junit.org/sponsoring")
            } else {
                logger.i("${failed("Failed")} Could not download JUnit platform (${response.status})")
                exitProcess(1)
            }
        }

        arg(pathFrom(jdkInstallation.path, "bin", "java").toString())
        args("-jar", junitConsoleJar.toString())
        args("-classpath", (dependencyArtifacts + artifactPath).joinToString(CPSEP))
        arg("--disable-banner") // TODO: Put a copy of the JUnit banner in the docs
        args("--scan-classpath", artifactPath)
        //args("--select-file", artifactPath)
        //args("--include-classname", ".*")
        //args("--select-class", "CLASS")
        //args("--select-method", "NAME")
        //args("--select-package", "PACKAGE")
        // TODO: Specify reports directory
        //args("--reports-dir", "DIR")
    }
}
