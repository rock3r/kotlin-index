package com.kotlincodeindex.topology.bazel

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildFileParserTest {
    @Test
    fun `parses kt_jvm_library srcs from BUILD snippet`() {
        val workspace = Path("src/test/resources/fixtures/bazel")
        val buildFile = workspace.resolve("plugins/foo/ui/BUILD.bazel")
        val paths = BuildFileParser.parseKotlinSources(buildFile, workspace)
        assertEquals(
            listOf(
                "plugins/foo/ui/src/main/kotlin/Panel.kt",
                "plugins/foo/ui/src/main/kotlin/Other.kt",
            ).sorted(),
            paths.sorted(),
        )
    }
}
