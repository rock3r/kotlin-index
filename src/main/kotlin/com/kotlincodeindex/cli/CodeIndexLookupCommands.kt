package com.kotlincodeindex.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.kotlincodeindex.core.git.GitHeadResolver
import com.kotlincodeindex.core.record.CodeIndexRecord
import com.kotlincodeindex.core.record.CodeIndexRecordCodec
import com.kotlincodeindex.core.record.ReferenceRecord
import com.kotlincodeindex.core.record.SymbolRecord
import com.kotlincodeindex.core.store.CodeIndexStore
import com.kotlincodeindex.core.store.IndexStoreOpener
import java.nio.file.Path

class FindSymbolCommand : CliktCommand(name = "find-symbol") {
    private val project by
        option("--project").file(mustExist = true, mustBeReadable = true).required()
    private val name by option("--name").required()
    private val kind by option("--kind")
    private val language by option("--language")
    private val sessionId by option("--session-id")
    private val format by option("--format").default(JSONL)

    override fun run() {
        requireJsonl(format)
        withStore(project.toPath(), sessionId) { store ->
            findSymbols(store, name, kind, language).forEach { echo(encode(it)) }
        }
    }

    internal fun findSymbols(
        store: CodeIndexStore,
        name: String,
        kind: String? = null,
        language: String? = null,
    ): List<SymbolRecord> =
        sequenceOf("sym:", "res:")
            .flatMap(store::prefixScan)
            .map { it.second }
            .filterIsInstance<SymbolRecord>()
            .filter { symbol ->
                (symbol.name == name || symbol.fqn == name || name in symbol.aliases) &&
                    (kind == null || symbol.kind == kind) &&
                    (language == null || symbol.language == language)
            }
            .sortedWith(
                compareBy(SymbolRecord::fqn, SymbolRecord::relativeFile, SymbolRecord::line)
            )
            .toList()
}

class FindReferencesCommand : CliktCommand(name = "find-references") {
    private val project by
        option("--project").file(mustExist = true, mustBeReadable = true).required()
    private val symbol by option("--symbol").required()
    private val sessionId by option("--session-id")
    private val format by option("--format").default(JSONL)

    override fun run() {
        requireJsonl(format)
        withStore(project.toPath(), sessionId) { store ->
            findReferences(store, symbol).forEach { echo(encode(it)) }
        }
    }

    internal fun findReferences(store: CodeIndexStore, symbol: String): List<ReferenceRecord> {
        val targetIds = linkedSetOf(symbol)
        store
            .prefixScan("sym:")
            .map { it.second }
            .filterIsInstance<SymbolRecord>()
            .filter { it.fqn == symbol || symbol in it.aliases }
            .forEach {
                targetIds += it.fqn
                targetIds += it.aliases
            }
        return store
            .prefixScan("ref:")
            .map { it.second }
            .filterIsInstance<ReferenceRecord>()
            .filter { reference ->
                reference.symbolFqn in targetIds ||
                    reference.candidateSymbolFqns.any { it in targetIds }
            }
            .sortedWith(
                compareBy(
                    ReferenceRecord::relativeFile,
                    ReferenceRecord::line,
                    ReferenceRecord::column,
                )
            )
            .toList()
    }
}

class ResolveResourceCommand : CliktCommand(name = "resolve-resource") {
    private val project by
        option("--project").file(mustExist = true, mustBeReadable = true).required()
    private val type by option("--type").required()
    private val name by option("--name").required()
    private val sessionId by option("--session-id")
    private val format by option("--format").default(JSONL)

    override fun run() {
        requireJsonl(format)
        withStore(project.toPath(), sessionId) { store ->
            resolveResources(store, type, name).forEach { echo(encode(it)) }
        }
    }

    internal fun resolveResources(
        store: CodeIndexStore,
        type: String,
        name: String,
    ): List<SymbolRecord> =
        store
            .prefixScan("res:$type:$name:")
            .map { it.second }
            .filterIsInstance<SymbolRecord>()
            .filter { it.fqn == "res:$type:$name" }
            .sortedWith(compareBy(SymbolRecord::relativeFile, SymbolRecord::line))
            .toList()
}

private fun <T> withStore(project: Path, sessionId: String?, block: (CodeIndexStore) -> T): T {
    val commit = GitHeadResolver.resolve(project)
    val store = IndexStoreOpener.openForQuery(project, commit, sessionId)
    return try {
        block(store)
    } finally {
        store.close()
    }
}

private fun encode(record: CodeIndexRecord): String =
    CodeIndexRecordCodec.encode(record).decodeToString()

private fun requireJsonl(format: String) {
    require(format == JSONL) { "Only jsonl format is supported" }
}

private const val JSONL = "jsonl"
