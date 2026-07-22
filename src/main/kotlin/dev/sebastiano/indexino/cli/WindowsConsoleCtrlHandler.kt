package dev.sebastiano.indexino.cli

import sun.misc.Signal

internal object WindowsConsoleCtrlHandler {
    fun install(
        osName: String = System.getProperty("os.name"),
        register: ((() -> Unit) -> Unit) = ::registerInterrupt,
        halt: (Int) -> Unit = Runtime.getRuntime()::halt,
    ) {
        if (!osName.startsWith("Windows", ignoreCase = true)) return
        register { halt(INTERRUPT_EXIT_CODE) }
    }

    private fun registerInterrupt(onInterrupt: () -> Unit) {
        Signal.handle(Signal("INT")) { onInterrupt() }
    }

    private const val INTERRUPT_EXIT_CODE = 130
}
