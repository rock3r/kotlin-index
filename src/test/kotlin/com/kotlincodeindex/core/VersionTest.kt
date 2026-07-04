package com.kotlincodeindex.core

import kotlin.test.Test
import kotlin.test.assertEquals

class VersionTest {
    @Test
    fun `version is defined`() {
        assertEquals("0.1.0-SNAPSHOT", Version.NAME)
    }
}
