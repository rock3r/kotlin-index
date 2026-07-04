package com.kotlincodeindex.producer

import com.kotlincodeindex.core.key.CodeIndexKey
import com.kotlincodeindex.core.record.FileHashRecord
import com.kotlincodeindex.core.store.CodeIndexStore
import java.security.MessageDigest

class FileHashProducer : IndexProducer {
    override val id: String = "file-hash"
    override val namespace: String = "file"
    override val displayName: String = "FileHashProducer"

    override fun produce(context: IndexBuildContext, store: CodeIndexStore) {
        for (relativePath in context.sourceFiles) {
            val content = context.readSource(relativePath)
            val hash = contentHash(content)
            store.put(
                CodeIndexKey.file(relativePath, hash),
                FileHashRecord(relativePath = relativePath, contentHash = hash),
            )
        }
    }

    companion object {
        fun contentHash(content: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
            return "sha256:" + digest.joinToString("") { "%02x".format(it) }
        }
    }
}
