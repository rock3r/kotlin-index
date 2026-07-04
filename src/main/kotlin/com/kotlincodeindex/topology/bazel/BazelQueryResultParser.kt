package com.kotlincodeindex.topology.bazel

object BazelQueryResultParser {
    fun parseKotlinSourcePaths(lines: Iterable<String>): List<String> =
        lines.mapNotNull { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("//") || !trimmed.endsWith(".kt")) {
                return@mapNotNull null
            }
            val withoutPrefix = trimmed.removePrefix("//")
            val separator = withoutPrefix.indexOf(':')
            if (separator <= 0) {
                return@mapNotNull null
            }
            withoutPrefix.substring(0, separator) + "/" + withoutPrefix.substring(separator + 1)
        }
}
