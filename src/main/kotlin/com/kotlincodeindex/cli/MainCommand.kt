package com.kotlincodeindex.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

/** Entry point for the kotlin-code-index CLI. */
class MainCommand : CliktCommand(name = "kotlin-code-index") {
    init {
        subcommands(
            IndexCommand(),
            QueryCommand(),
            StatusCommand(),
            FindSymbolCommand(),
            FindReferencesCommand(),
            ResolveResourceCommand(),
        )
    }

    override fun run() = Unit
}

fun main(args: Array<String>) {
    MainCommand().main(args)
}
