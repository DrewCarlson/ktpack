package ktpack

import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.github.ajalt.mordant.terminal.*
import ktpack.commands.*
import ktpack.commands.jdk.*
import ktpack.commands.kotlin.*
import ktpack.commands.nodejs.InstallNodejsCommand
import ktpack.commands.nodejs.ListNodejsCommand
import ktpack.commands.nodejs.NodejsCommand
import ktpack.commands.nodejs.RemoveNodejsCommand
import ktpack.util.exitProcess
import ktpack.util.failed

fun main(args: Array<String>) {
    val term = Terminal()
    val command = KtpackCommand(term).apply {
        subcommands(
            CheckCommand(),
            BuildCommand(),
            RunCommand(),
            TestCommand(),
            NewCommand(),
            InitCommand(),
            CleanCommand(),
            DocCommand(),
            VersionCommand(),
            DependenciesCommand(),
            PublishCommand(),
            SetupCommand(),
            KotlinCommand()
                .subcommands(InstallKotlinCommand())
                .subcommands(RemoveKotlinCommand())
                .subcommands(FindKotlinCommand())
                .subcommands(ListKotlinCommand()),
            JdkCommand()
                .subcommands(InstallJdkCommand())
                .subcommands(RemoveJdkCommand())
                .subcommands(FindJdkCommand())
                .subcommands(ListJdkCommand()),
            NodejsCommand()
                .subcommands(InstallNodejsCommand())
                .subcommands(RemoveNodejsCommand())
                .subcommands(ListNodejsCommand()),
        )
    }

    try {
        command.main(args)
    } catch (e: Throwable) {
        term.println()
        term.println("${failed("Failed")} Uncaught error: ${e.message}")
        if (command.stacktrace) {
            term.println(e.stackTraceToString())
        }
        if (!command.debug || !command.stacktrace) {
            term.println()
            term.println("Run the command again with '--debug --stacktrace' or '-ds' to see more details. (example: ktpack -ds test)")
            term.println("Submit the logs with your issue report @ ${underline("https://github.com/DrewCarlson/ktpack/issues/new")}")
            term.println()
        }
        exitProcess(1)
    }
}
