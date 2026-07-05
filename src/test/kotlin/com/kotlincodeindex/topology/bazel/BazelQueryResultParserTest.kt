package com.kotlincodeindex.topology.bazel

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class BazelQueryResultParserTest {
    @Test
    fun `parses mock query output to kotlin source paths`() {
        val lines =
            Path("src/test/resources/fixtures/bazel/mock-query-output.txt").toFile().readLines()
        val paths = BazelQueryResultParser.parseKotlinSourcePaths(lines)
        assertEquals(
            listOf(
                "plugins/foo/ui/src/main/kotlin/Panel.kt",
                "plugins/foo/ui/src/main/kotlin/Other.kt",
            ),
            paths,
        )
    }
}
