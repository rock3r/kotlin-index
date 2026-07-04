package com.kotlincodeindex.topology.bazel

import java.nio.file.Path
import kotlin.io.path.readText

object BuildFileParser {
    fun parseKotlinSources(buildFile: Path, workspaceRoot: Path): List<String> {
        val packageDir = checkNotNull(buildFile.parent) { "BUILD file has no parent: $buildFile" }
        val packageRelative = workspaceRoot.relativize(packageDir).toString().replace('\\', '/')
        val srcs = mutableListOf<String>()
        var inSrcs = false

        for (line in buildFile.readText().lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("srcs") && trimmed.contains("=") -> inSrcs = true
                inSrcs && trimmed.startsWith("\"") &&
                    (trimmed.endsWith("\",") || trimmed.endsWith("\"")) -> {
                    val entry = trimmed.trim('"', ',')
                    if (entry.endsWith(".kt")) {
                        srcs += "$packageRelative/$entry"
                    }
                }
                inSrcs && (trimmed == "]," || trimmed == "]") -> inSrcs = false
            }
        }
        return srcs
    }
}
