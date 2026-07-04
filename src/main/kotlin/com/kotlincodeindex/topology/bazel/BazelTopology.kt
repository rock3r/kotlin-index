package com.kotlincodeindex.topology.bazel

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object BazelTopology {
    fun defaultExecutor(): BazelQueryExecutor = BazelQueryExecutor { target, workspace ->
        if (isBazelAvailable()) {
            liveQuery(target, workspace)
        } else {
            degradedQuery(target, workspace)
        }
    }

    fun resolveSources(
        target: String,
        workspace: Path,
        executor: BazelQueryExecutor = defaultExecutor(),
    ): TopologyResult {
        val lines = executor.query(target, workspace)
        return TopologyResult(
            sourceFiles = BazelQueryResultParser.parseKotlinSourcePaths(lines),
            topology = resolveTopology(executor),
        )
    }

    private fun resolveTopology(executor: BazelQueryExecutor): String = when {
        executor is MockBazelQueryExecutor -> "bazel-query"
        isBazelAvailable() -> "bazel-query"
        else -> "build-parse"
    }

    private fun isBazelAvailable(): Boolean =
        runCatching {
            ProcessBuilder("bazel", "version")
                .redirectErrorStream(true)
                .start()
                .waitFor() == 0
        }.getOrDefault(false)

    private fun liveQuery(target: String, workspace: Path): List<String> {
        val query = "kind('source file', deps($target))"
        val process = ProcessBuilder("bazel", "query", query, "--output=label")
            .directory(workspace.toFile())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readLines()
        check(process.waitFor() == 0) { "bazel query failed: ${output.joinToString("\n")}" }
        return output
    }

    private fun degradedQuery(target: String, workspace: Path): List<String> =
        degradedSourceLabels(target, workspace)

    fun degradedSourceLabels(target: String, workspace: Path): List<String> {
        val packagePath = target.removePrefix("//").substringBefore(':')
        val packageDir = workspace.resolve(packagePath)
        check(packageDir.isDirectory()) { "Package directory not found for target $target: $packageDir" }
        val buildFile = sequenceOf("BUILD.bazel", "BUILD")
            .map { packageDir.resolve(it) }
            .firstOrNull { it.exists() }
            ?: error("No BUILD file under $packageDir")
        return BuildFileParser.parseKotlinSources(buildFile, workspace).map { relativePath ->
            val filePart = relativePath.removePrefix("$packagePath/")
            "//$packagePath:$filePart"
        }
    }
}

data class TopologyResult(
    val sourceFiles: List<String>,
    val topology: String,
)
