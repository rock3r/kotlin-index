package com.kotlincodeindex.topology.bazel

import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildFileParserTest {
    @Test
    fun `parses kt_jvm_library srcs from BUILD snippet`() {
        val workspace = Path("src/test/resources/fixtures/bazel")
        val buildFile = workspace.resolve("plugins/foo/ui/BUILD.bazel")
        val result = BuildFileParser.parseKotlinSources(buildFile, workspace)
        assertEquals(
            listOf(
                "plugins/foo/ui/src/main/kotlin/Panel.kt",
                "plugins/foo/ui/src/main/kotlin/Other.kt",
            ).sorted(),
            result.paths.sorted(),
        )
    }

    @Test
    fun `glob exclude patterns are not indexed as sources`() {
        val workspace = Path("src/test/resources/fixtures/bazel")
        val buildFile = workspace.resolve("plugins/foo/glob-exclude/BUILD.bazel")
        val result = BuildFileParser.parseKotlinSources(buildFile, workspace)
        assertEquals(
            listOf("plugins/foo/glob-exclude/src/main/kotlin/Main.kt"),
            result.paths.sorted(),
        )
    }

    @Test
    fun `nested exclude glob patterns are not indexed as sources`() {
        val workspace = createTempDirectory("build-file-parser-exclude-glob-")
        val packageDir = workspace.resolve("pkg")
        packageDir.toFile().mkdirs()
        packageDir.resolve("src/main/kotlin/Main.kt").toFile().apply {
            parentFile.mkdirs()
            writeText("class Main")
        }
        packageDir.resolve("src/main/kotlin/MainTest.kt").toFile().writeText("class MainTest")
        packageDir.resolve("BUILD.bazel").writeText(
            """
            kt_jvm_library(
                name = "lib",
                srcs = glob(
                    ["**/*.kt"],
                    exclude = glob(["**/*Test.kt"]),
                ),
            )
            """.trimIndent(),
        )

        val result = BuildFileParser.parseKotlinSources(packageDir.resolve("BUILD.bazel"), workspace)
        assertEquals(listOf("pkg/src/main/kotlin/Main.kt"), result.paths)
    }
}
