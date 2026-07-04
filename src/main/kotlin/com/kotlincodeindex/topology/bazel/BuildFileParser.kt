package com.kotlincodeindex.topology.bazel

import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

data class BuildParseResult(
    val paths: List<String>,
    val warnings: List<String>,
)

object BuildFileParser {
    private val GLOB_BLOCK = Regex("""glob\s*\(\s*\[([\s\S]*?)]""", RegexOption.MULTILINE)
    private val QUOTED_STRING = Regex(""""([^"]+)"""")
    private val LITERAL_SRCS_BLOCK = Regex("""srcs\s*=\s*\[([\s\S]*?)]""", RegexOption.MULTILINE)

    fun parseKotlinSources(buildFile: Path, workspaceRoot: Path): BuildParseResult {
        val packageDir = checkNotNull(buildFile.parent) { "BUILD file has no parent: $buildFile" }
        val packageRelative = workspaceRoot.relativize(packageDir).toString().replace('\\', '/')
        val content = buildFile.readText()
        val paths = linkedSetOf<String>()
        val warnings = mutableListOf<String>()

        for (pattern in extractGlobPatterns(content)) {
            val expanded = expandGlob(packageDir, pattern)
            val ktFiles = expanded.filter { it.endsWith(".kt") }
            if (ktFiles.isEmpty() && pattern.contains(".kt")) {
                warnings += "BUILD glob '$pattern' under $packageRelative matched no .kt files"
            }
            ktFiles.forEach { relative ->
                paths += "$packageRelative/$relative"
            }
        }

        for (literal in extractLiteralSrcs(content)) {
            if (literal.endsWith(".kt") && !literal.contains('*')) {
                paths += "$packageRelative/$literal"
            }
        }

        return BuildParseResult(paths.toList(), warnings)
    }

    private fun extractGlobPatterns(content: String): List<String> =
        GLOB_BLOCK.findAll(content).flatMap { match ->
            QUOTED_STRING.findAll(match.groupValues[1]).map { it.groupValues[1] }
        }.toList()

    private fun extractLiteralSrcs(content: String): List<String> {
        val globRanges = GLOB_BLOCK.findAll(content).map { it.range }.toList()
        return LITERAL_SRCS_BLOCK.findAll(content).flatMap { match ->
            if (match.groupValues[1].contains("glob(")) {
                emptySequence()
            } else {
                QUOTED_STRING.findAll(match.groupValues[1])
                    .map { it.groupValues[1] }
                    .filter { quoted ->
                        val quoteRange = match.range.first + match.groupValues[1].indexOf("\"$quoted\"")
                        globRanges.none { quoteRange in it }
                    }
            }
        }.toList()
    }

    private fun expandGlob(packageDir: Path, pattern: String): List<String> {
        if (!pattern.contains('*') && !pattern.contains('?')) {
            val file = packageDir.resolve(pattern)
            return if (file.isRegularFile()) listOf(pattern) else emptyList()
        }
        if (!packageDir.exists()) {
            return emptyList()
        }
        val matcher = packageDir.fileSystem.getPathMatcher("glob:$pattern")
        return Files.walk(packageDir, FileVisitOption.FOLLOW_LINKS).use { stream ->
            stream.filter { it.isRegularFile() }
                .map { packageDir.relativize(it).toString().replace('\\', '/') }
                .filter { matcher.matches(Path.of(it)) }
                .sorted()
                .toList()
        }
    }
}
