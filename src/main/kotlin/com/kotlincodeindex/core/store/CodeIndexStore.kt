package com.kotlincodeindex.core.store

import com.kotlincodeindex.core.key.CodeIndexKey
import com.kotlincodeindex.core.record.CodeIndexRecord

interface CodeIndexStore {
    fun get(key: CodeIndexKey): CodeIndexRecord?

    fun put(key: CodeIndexKey, record: CodeIndexRecord)

    fun delete(key: CodeIndexKey)

    fun prefixScan(prefix: String): Sequence<Pair<CodeIndexKey, CodeIndexRecord>>

    fun <T> transaction(block: () -> T): T

    fun close()
}
