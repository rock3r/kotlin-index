package com.kotlincodeindex.topology.bazel

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object BazelTopology {
    fun defaultExecutor(onStderr: (String) -> Unit = { System.err.println(it) }): BazelQueryExecutor =
        BazelQueryExecutor { target, workspace ->
            if (isBazelAvailable()) {
                queryWithFallback(target, workspace, LiveBazelProcessRunner, onStderr)
            } else {
                degradedQuery(target, workspace, onStderr)
            }
        }

    fun resolveSources(
        target: String,
        workspace: Path,
        executor: BazelQueryExecutor? = null,
        onStderr: (String) -> Unit = { System.err.println(it) },
    ): TopologyResult {
        val exec = executor ?: defaultExecutor(onStderr)
        val lines = exec.query(target, workspace)
        return TopologyResult(
            sourceFiles = BazelQueryResultParser.parseKotlinSourcePaths(lines),
            topology = resolveTopology(exec),
        )
    }

    fun queryWithFallback(
        target: String,
        workspace: Path,
        runner: BazelProcessRunner = LiveBazelProcessRunner,
        onStderr: (String) -> Unit = { System.err.println(it) },
    ): List<String> {
        val primaryQuery = "kind('source file', deps($target))"
        val primary = runner.run(primaryQuery, workspace)
        if (primary.exitCode == 0) {
            return primary.lines
        }

        onStderr(
            "bazel query failed ($primaryQuery); retrying with labels(srcs, $target)",
        )
        val fallbackQuery = "labels(srcs, $target)"
        val fallback = runner.run(fallbackQuery, workspace)
        check(fallback.exitCode == 0) {
            "bazel query failed: ${fallback.lines.joinToString("\n")}"
        }
        return fallback.lines
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

    private fun degradedQuery(
        target: String,
        workspace: Path,
        onStderr: (String) -> Unit,
    ): List<String> = degradedSourceLabels(target, workspace, onStderr)

    fun degradedSourceLabels(
        target: String,
        workspace: Path,
        onStderr: (String) -> Unit = { System.err.println(it) },
    ): List<String> {
        val packagePath = target.removePrefix("//").substringBefore(':')
        val packageDir = workspace.resolve(packagePath)
        check(packageDir.isDirectory()) { "Package directory not found for target $target: $packageDir" }
        val buildFile = sequenceOf("BUILD.bazel", "BUILD")
            .map { packageDir.resolve(it) }
            .firstOrNull { it.exists() }
            ?: error("No BUILD file under $packageDir")
        val parseResult = BuildFileParser.parseKotlinSources(buildFile, workspace)
        parseResult.warnings.forEach(onStderr)
        if (parseResult.paths.isEmpty()) {
            onStderr("build-parse: no Kotlin sources found for $target under $packagePath")
        }
        return parseResult.paths.map { relativePath ->
            val filePart = relativePath.removePrefix("$packagePath/")
            "//$packagePath:$filePart"
        }
    }
}

data class TopologyResult(
    val sourceFiles: List<String>,
    val topology: String,
)
