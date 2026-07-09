package com.kotlincodeindex.cli

import com.github.ajalt.clikt.testing.test
import com.kotlincodeindex.core.key.CodeIndexKey
import com.kotlincodeindex.core.manifest.IndexManifest
import com.kotlincodeindex.core.manifest.ManifestIO
import com.kotlincodeindex.core.path.IndexPathResolver
import com.kotlincodeindex.core.record.ReferenceRecord
import com.kotlincodeindex.core.record.SymbolRecord
import com.kotlincodeindex.core.xodus.XodusCodeIndexStore
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeIndexLookupCommandTest {
    private val tempDirs = mutableListOf<java.nio.file.Path>()

    @AfterTest
    fun tearDown() {
        tempDirs.forEach { it.toFile().deleteRecursively() }
    }

    @Test
    fun `find symbol emits deterministic machine readable rows`() {
        val workspace = indexedWorkspace()
        val result = runCli("find-symbol", "--project", workspace.toString(), "--name", "Panel")

        assertEquals(0, result.statusCode, result.output)
        assertTrue(
            result.output.lineSequence().filter { it.isNotBlank() }.all { it.startsWith("{") }
        )
        assertTrue(result.output.contains("\"fqn\":\"sample.Panel\""))
        assertTrue(result.output.contains("\"language\":\"java\""))
    }

    @Test
    fun `find references resolves a language neutral member id`() {
        val workspace = indexedWorkspace()
        val result =
            runCli(
                "find-references",
                "--project",
                workspace.toString(),
                "--symbol",
                "sample.Panel#render",
            )

        assertEquals(0, result.statusCode, result.output)
        assertTrue(result.output.contains("\"symbolFqn\":\"sample.Panel#render\""))
        assertTrue(result.output.contains("\"relativeFile\":\"src/App.kt\""))
    }

    @Test
    fun `lookups join Kotlin source ids with generated JVM facade aliases`() {
        val workspace = indexedWorkspace()
        val symbolResult =
            runCli(
                "find-symbol",
                "--project",
                workspace.toString(),
                "--name",
                "sample.KotlinApiKt#greet",
            )
        val referencesResult =
            runCli("find-references", "--project", workspace.toString(), "--symbol", "sample.greet")

        assertEquals(0, symbolResult.statusCode, symbolResult.output)
        assertTrue(symbolResult.output.contains("\"fqn\":\"sample.greet\""))
        assertEquals(0, referencesResult.statusCode, referencesResult.output)
        assertTrue(referencesResult.output.contains("\"symbolFqn\":\"sample.KotlinApiKt#greet\""))
    }

    @Test
    fun `resolve resource disambiguates by type and name`() {
        val workspace = indexedWorkspace()
        val result =
            runCli(
                "resolve-resource",
                "--project",
                workspace.toString(),
                "--type",
                "string",
                "--name",
                "title",
            )

        assertEquals(0, result.statusCode, result.output)
        assertTrue(result.output.contains("\"fqn\":\"res:string:title\""))
        assertTrue(result.output.contains("values/strings.xml"))
    }

    private fun indexedWorkspace(): java.nio.file.Path {
        val workspace = createTempDirectory("lookup-cli-")
        tempDirs.add(workspace)
        runGit(workspace, "init")
        runGit(workspace, "config", "user.email", "test@example.com")
        runGit(workspace, "config", "user.name", "Test User")
        workspace.resolve("README").toFile().writeText("fixture")
        runGit(workspace, "add", ".")
        runGit(workspace, "commit", "-m", "fixture")
        val commit = runGit(workspace, "rev-parse", "HEAD").trim()
        val resolver = IndexPathResolver(workspace)
        val store = XodusCodeIndexStore.open(resolver.resolveBaseStore(commit))
        try {
            populateLookupRecords(store)
        } finally {
            store.close()
        }
        ManifestIO.write(
            resolver.resolveManifest(commit),
            IndexManifest(
                commit = commit,
                indexerVersion = "test",
                scope = ":",
                topology = "gradle-parse",
                sourceFileCount = 3,
                sourcesContentHash = "sha256:test",
                builtAt = "2026-01-01T00:00:00Z",
            ),
        )
        return workspace
    }

    private fun populateLookupRecords(store: XodusCodeIndexStore) {
        val symbol =
            SymbolRecord(
                fqn = "sample.Panel",
                relativeFile = "src/Panel.java",
                line = 3,
                kind = "class",
                name = "Panel",
                language = "java",
            )
        store.put(
            CodeIndexKey.symbolDefinition(symbol.fqn, symbol.relativeFile, symbol.line, 1),
            symbol,
        )
        val reference =
            ReferenceRecord(
                symbolFqn = "sample.Panel#render",
                relativeFile = "src/App.kt",
                line = 8,
                column = 5,
                language = "kotlin",
            )
        store.put(
            CodeIndexKey.ref(
                reference.symbolFqn,
                reference.relativeFile,
                reference.line,
                reference.column,
            ),
            reference,
        )
        val topLevelFunction =
            SymbolRecord(
                fqn = "sample.greet",
                relativeFile = "src/KotlinApi.kt",
                line = 3,
                kind = "function",
                name = "greet",
                language = "kotlin",
                aliases = listOf("sample.KotlinApiKt#greet"),
            )
        store.put(
            CodeIndexKey.symbolDefinition(
                topLevelFunction.fqn,
                topLevelFunction.relativeFile,
                topLevelFunction.line,
                1,
            ),
            topLevelFunction,
        )
        val facadeReference =
            ReferenceRecord(
                symbolFqn = "sample.KotlinApiKt#greet",
                relativeFile = "src/JavaCaller.java",
                line = 6,
                column = 9,
                language = "java",
            )
        store.put(
            CodeIndexKey.ref(
                facadeReference.symbolFqn,
                facadeReference.relativeFile,
                facadeReference.line,
                facadeReference.column,
            ),
            facadeReference,
        )
        val resource =
            SymbolRecord(
                fqn = "res:string:title",
                relativeFile = "app/src/main/res/values/strings.xml",
                line = 2,
                kind = "resource",
                name = "title",
                language = "xml",
                ownerFqn = "res:string",
            )
        store.put(
            CodeIndexKey.resource("string", "title", resource.relativeFile, resource.line),
            resource,
        )
    }

    private fun runCli(vararg args: String): CliResult {
        val result = MainCommand().test(args.toList())
        return CliResult(result.output, result.statusCode)
    }

    private fun runGit(workspace: java.nio.file.Path, vararg args: String): String {
        val process =
            ProcessBuilder(*listOf("git", "-C", workspace.toString(), *args).toTypedArray())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "git ${args.joinToString(" ")} failed: $output" }
        return output
    }

    private data class CliResult(val output: String, val statusCode: Int)
}
