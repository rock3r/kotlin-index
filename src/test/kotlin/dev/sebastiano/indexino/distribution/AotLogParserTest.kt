package dev.sebastiano.indexino.distribution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AotLogParserTest {
    @Test
    fun `accepted cache exposes semantic facts without depending on timing prefixes`() {
        val facts =
            AotLogParser.parse(
                """
                [0.012s][info][aot] Opened AOT cache /relocated/runtime/lib/server/classes.jsa.
                [0.034s][info][aot] Using AOT-linked classes: true
                """
                    .trimIndent()
            )

        assertEquals("/relocated/runtime/lib/server/classes.jsa", facts.cachePath)
        assertTrue(facts.accepted)
        assertFalse(facts.rejected)
        assertEquals(true, facts.linkedClasses)
    }

    @Test
    fun `rejected cache is recognized independently of incidental reason wording`() {
        val facts =
            AotLogParser.parse(
                """
                [0.004s][warning][aot] Unable to use AOT cache C:\app\runtime\bin\server\classes.jsa: header is invalid
                [0.005s][info][aot] Using AOT-linked classes: false
                """
                    .trimIndent()
            )

        assertEquals("C:\\app\\runtime\\bin\\server\\classes.jsa", facts.cachePath)
        assertFalse(facts.accepted)
        assertTrue(facts.rejected)
        assertEquals(false, facts.linkedClasses)
    }
}
