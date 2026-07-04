package com.kotlincodeindex.topology.bazel

import java.nio.file.Path

fun interface BazelQueryExecutor {
    fun query(target: String, workspace: Path): List<String>
}

class MockBazelQueryExecutor(
    private val lines: List<String>,
) : BazelQueryExecutor {
    override fun query(target: String, workspace: Path): List<String> = lines
}
