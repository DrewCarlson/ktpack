package ktpack

import com.github.ajalt.clikt.core.*
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

        // runBlocking { command.taskRunner.execute() }
    } catch (e: Throwable) {
        term.println(
            buildString {
                append(failed("Failed"))
                append(" Uncaught error: ")
                e.message?.let(::append)
            },
        )
        if (command.stacktrace) {
            term.println(e.stackTraceToString())
        }
        exitProcess(1)
    }
}
