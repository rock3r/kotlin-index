package dev.sebastiano.indexino.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class WindowsConsoleCtrlHandlerTest {
    @Test
    fun `windows interrupt handler halts with a nonzero conventional exit code`() {
        var installed: (() -> Unit)? = null
        var exitCode: Int? = null

        WindowsConsoleCtrlHandler.install(
            osName = "Windows 11",
            register = { installed = it },
            halt = { exitCode = it },
        )

        assertNotNull(installed).invoke()
        assertEquals(130, exitCode)
    }

    @Test
    fun `non-windows launch does not install a console interrupt handler`() {
        var registered = false
        var halted = false

        WindowsConsoleCtrlHandler.install(
            osName = "Linux",
            register = { registered = true },
            halt = { halted = true },
        )

        assertFalse(registered)
        assertFalse(halted)
    }
}
