package com.kotlincodeindex.producer.xml

import com.kotlincodeindex.core.record.ReferenceRecord
import com.kotlincodeindex.core.record.SymbolRecord
import com.kotlincodeindex.core.xodus.XodusCodeIndexStore
import com.kotlincodeindex.producer.IndexBuildContext
import com.kotlincodeindex.producer.ProducerRegistry
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class XmlResourceProducerTest {
    @Test
    fun `indexes file values and id resources with references`() {
        val values =
            """
            <resources>
                <string name="title">Hello</string>
                <item type="color" name="accent">#ff0000</item>
            </resources>
            """
                .trimIndent()
        val layout =
            """
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android">
                <TextView
                    android:id="@+id/title_view"
                    android:text="@string/title" />
            </LinearLayout>
            """
                .trimIndent()

        withStore { store ->
            val producer = assertNotNull(ProducerRegistry.get("xml-resources"))
            producer.produce(
                IndexBuildContext.forInlineSources(
                    store = store,
                    commitHash = "abc",
                    sourceFiles =
                        mapOf(
                            "app/src/main/res/values/strings.xml" to values,
                            "app/src/main/res/layout/main_screen.xml" to layout,
                        ),
                )
            )

            val resources =
                store.prefixScan("res:").map { it.second }.filterIsInstance<SymbolRecord>().toList()
            assertTrue(resources.any { it.fqn == "res:string:title" })
            assertTrue(resources.any { it.fqn == "res:color:accent" })
            assertTrue(resources.any { it.fqn == "res:layout:main_screen" })
            assertTrue(resources.any { it.fqn == "res:id:title_view" })

            val references =
                store
                    .prefixScan("ref:")
                    .map { it.second }
                    .filterIsInstance<ReferenceRecord>()
                    .toList()
            assertTrue(
                references.any { it.symbolFqn == "res:string:title" && it.context == "resource" }
            )
        }
    }

    @Test
    fun `rejects external entity expansion`() {
        val malicious =
            """
            <!DOCTYPE resources [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
            <resources><string name="title">&secret;</string></resources>
            """
                .trimIndent()

        withStore { store ->
            val producer = assertNotNull(ProducerRegistry.get("xml-resources"))
            val failure =
                runCatching {
                        producer.produce(
                            IndexBuildContext.forInlineSources(
                                store = store,
                                commitHash = "abc",
                                sourceFiles =
                                    mapOf("app/src/main/res/values/strings.xml" to malicious),
                            )
                        )
                    }
                    .exceptionOrNull()
            assertNotNull(failure)
            assertTrue(failure.message.orEmpty().contains("strings.xml"))
        }
    }

    private fun withStore(block: (XodusCodeIndexStore) -> Unit) {
        val store =
            XodusCodeIndexStore.open(createTempDirectory("xml-resource-producer-").resolve("index"))
        try {
            block(store)
        } finally {
            store.close()
        }
    }
}
