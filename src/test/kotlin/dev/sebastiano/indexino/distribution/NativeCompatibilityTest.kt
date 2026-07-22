package dev.sebastiano.indexino.distribution

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir

@Tag("native-distribution")
class NativeCompatibilityTest {
    @TempDir lateinit var tempDir: Path

    @Test
    fun `all distribution entry points preserve the golden CLI contract`() {
        val target = requiredProperty("indexino.nativeTarget")
        val java =
            requiredDirectory("indexino.targetJdkRoot")
                .resolve("bin")
                .resolve(if (target == WINDOWS_X64) "java.exe" else "java")
        val thinClasspath = requiredProperty("indexino.thinRuntimeClasspath")
        val entryPoints =
            listOf(
                EntryPoint(
                    "thin",
                    listOf(
                        java.toString(),
                        "-Duser.home=${tempDir.resolve("home-thin")}",
                        "--enable-native-access=ALL-UNNAMED",
                        "-cp",
                        thinClasspath,
                        MAIN_CLASS,
                    ),
                ),
                EntryPoint(
                    "fat",
                    listOf(
                        java.toString(),
                        "-Duser.home=${tempDir.resolve("home-fat")}",
                        "--enable-native-access=ALL-UNNAMED",
                        "-jar",
                        requiredFile("indexino.unshrunkJar").toString(),
                    ),
                ),
                EntryPoint(
                    "r8",
                    listOf(
                        java.toString(),
                        "-Duser.home=${tempDir.resolve("home-r8")}",
                        "--enable-native-access=ALL-UNNAMED",
                        "-jar",
                        requiredFile("indexino.r8Jar").toString(),
                    ),
                ),
                EntryPoint(
                    "roast-$target",
                    listOf(
                        extractArchive(
                                requiredFile("indexino.nativeArchive"),
                                target,
                                "compatibility-install",
                            )
                            .resolve(if (target == WINDOWS_X64) "indexino.exe" else "indexino")
                            .toString()
                    ),
                ),
            )

        val template = createFixtureWorkspace()
        val caller = tempDir.resolve("compatibility-caller").createDirectories()
        val initialIndex = run(entryPoints.first(), caller, *indexArguments(template))
        assertEquals(0, initialIndex.exitCode, initialIndex.diagnostic())
        val commit = runCommand(template, "git", "rev-parse", "HEAD").stdout.trim()
        val manifest = template.resolve(".indexino/index/$commit/manifest.json")
        assertManifestSchema(manifest)
        val expectedManifest = manifest.readText()

        val snapshots = entryPoints.associate { entryPoint ->
            val workspace = tempDir.resolve("workspace-${entryPoint.name}")
            copyTree(template, workspace)
            entryPoint.name to snapshot(entryPoint, caller, workspace, commit, expectedManifest)
        }

        val golden = snapshots.getValue("thin")
        snapshots.forEach { (name, snapshot) ->
            assertEquals(golden.results, snapshot.results, "$name CLI behavior differs")
            assertEquals(golden.manifest, snapshot.manifest, "$name manifest differs")
        }
    }

    private fun snapshot(
        entryPoint: EntryPoint,
        caller: Path,
        workspace: Path,
        commit: String,
        expectedManifest: String,
    ): Snapshot {
        val results =
            linkedMapOf(
                "help" to run(entryPoint, caller, "--help"),
                "invalid" to run(entryPoint, caller, "not-a-command"),
                "fresh-index" to run(entryPoint, caller, *indexArguments(workspace)),
                "status" to run(entryPoint, caller, "status", "--project", workspace.toString()),
                "symbol-jsonl" to
                    run(
                        entryPoint,
                        caller,
                        "find-symbol",
                        "--project",
                        workspace.toString(),
                        "--name",
                        "Renderer",
                    ),
                "query-jsonl" to
                    run(
                        entryPoint,
                        caller,
                        "query",
                        "--project",
                        workspace.toString(),
                        "--application",
                        "selection-context",
                        "--preset",
                        "interactive-in-sc",
                        "--format",
                        "jsonl",
                    ),
            )
        assertEquals(0, results.getValue("help").exitCode, results.getValue("help").diagnostic())
        assertTrue(
            results.getValue("invalid").exitCode != 0,
            results.getValue("invalid").diagnostic(),
        )
        results
            .filterKeys { it != "invalid" }
            .forEach { (_, result) -> assertEquals(0, result.exitCode, result.diagnostic()) }

        val manifest = workspace.resolve(".indexino/index/$commit/manifest.json")
        assertManifestSchema(manifest)
        assertEquals(expectedManifest, manifest.readText(), "Fresh indexing changed the manifest")
        assertTrue(Files.isDirectory(manifest.parent.resolve("base.xodus")))
        return Snapshot(
            results.mapValues { (_, result) -> result.normalized(workspace) },
            manifest.readText(),
        )
    }

    private fun assertManifestSchema(manifest: Path) {
        val json = Json.parseToJsonElement(manifest.readText()).jsonObject
        assertEquals(
            setOf(
                "commit",
                "indexerVersion",
                "scope",
                "topology",
                "includeDeps",
                "sourceFileCount",
                "sourcesContentHash",
                "builtAt",
                "applications",
            ),
            json.keys,
        )
        assertEquals(
            requiredProperty("indexino.version"),
            json.getValue("indexerVersion").jsonPrimitive.content,
        )
    }

    private fun indexArguments(workspace: Path): Array<String> =
        arrayOf(
            "index",
            "--project",
            workspace.toString(),
            "--build-system",
            "gradle",
            "--gradle-module",
            ":app",
            "--applications",
            "selection-context",
        )

    private fun createFixtureWorkspace(): Path {
        val workspace = tempDir.resolve("compatibility-template")
        workspace.resolve("app/src/main/kotlin/sample").createDirectories()
        workspace
            .resolve("settings.gradle.kts")
            .writeText("rootProject.name = \"compatibility\"\ninclude(\":app\")\n")
        workspace
            .resolve("app/src/main/kotlin/sample/Panel.kt")
            .writeText(
                """
                package sample

                class Renderer {
                    fun render() = Unit
                }

                @Composable
                fun Content() {
                    SelectionContainer { ActionButton() }
                }
                """
                    .trimIndent()
            )
        runCommand(workspace, "git", "init")
        runCommand(workspace, "git", "config", "user.email", "distribution-test@example.invalid")
        runCommand(workspace, "git", "config", "user.name", "Distribution Test")
        runCommand(workspace, "git", "add", ".")
        val commit =
            runCommand(workspace, "git", "-c", "commit.gpgSign=false", "commit", "-m", "fixture")
        assertEquals(0, commit.exitCode, commit.diagnostic())
        return workspace
    }

    private fun copyTree(source: Path, destination: Path) {
        Files.walk(source).use { paths ->
            paths.forEach { path ->
                val target = destination.resolve(source.relativize(path).toString())
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target)
                } else {
                    Files.copy(
                        path,
                        target,
                        StandardCopyOption.COPY_ATTRIBUTES,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                }
            }
        }
    }

    private fun extractArchive(archive: Path, target: String, name: String): Path {
        val destination = tempDir.resolve(name).createDirectories()
        val result =
            when (target) {
                MACOS_ARM64 ->
                    runCommand(
                        destination,
                        "/usr/bin/ditto",
                        "-x",
                        "-k",
                        archive.toString(),
                        destination.toString(),
                    )
                LINUX_X64 ->
                    runCommand(
                        destination,
                        "unzip",
                        "-q",
                        archive.toString(),
                        "-d",
                        destination.toString(),
                    )
                WINDOWS_X64 ->
                    runCommand(
                        destination,
                        "powershell.exe",
                        "-NoProfile",
                        "-NonInteractive",
                        "-Command",
                        "Expand-Archive -LiteralPath '${powershellQuote(archive)}' " +
                            "-DestinationPath '${powershellQuote(destination)}' -Force",
                    )
                else -> error("Unsupported native target: $target")
            }
        assertEquals(0, result.exitCode, result.diagnostic())
        return destination.resolve("indexino")
    }

    private fun run(
        entryPoint: EntryPoint,
        workingDirectory: Path,
        vararg arguments: String,
    ): ProcessResult =
        runCommand(workingDirectory, *(entryPoint.commandPrefix + arguments).toTypedArray())

    private fun runCommand(workingDirectory: Path, vararg command: String): ProcessResult {
        val process =
            ProcessBuilder(*command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(false)
                .start()
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val stdout = executor.submit<String> { process.inputStream.bufferedReader().readText() }
            val stderr = executor.submit<String> { process.errorStream.bufferedReader().readText() }
            assertTrue(process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES))
            return ProcessResult(process.exitValue(), stdout.get(), stderr.get())
        }
    }

    private fun requiredProperty(name: String): String =
        requireNotNull(System.getProperty(name)) { "Missing $name" }

    private fun requiredFile(name: String): Path =
        Path.of(requiredProperty(name)).also { assertTrue(Files.isRegularFile(it), "Missing $it") }

    private fun requiredDirectory(name: String): Path =
        Path.of(requiredProperty(name)).also { assertTrue(Files.isDirectory(it), "Missing $it") }

    private fun powershellQuote(value: Any): String = value.toString().replace("'", "''")

    private data class EntryPoint(val name: String, val commandPrefix: List<String>)

    private data class Snapshot(val results: Map<String, ProcessResult>, val manifest: String)

    private data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String) {
        fun normalized(workspace: Path): ProcessResult {
            fun normalize(value: String): String =
                value
                    .replace(workspace.toAbsolutePath().toString(), "<workspace>")
                    .replace('\\', '/')
            return copy(stdout = normalize(stdout), stderr = normalize(stderr))
        }

        fun diagnostic(): String = "exit=$exitCode\nstdout:\n$stdout\nstderr:\n$stderr"
    }

    private companion object {
        const val MAIN_CLASS = "dev.sebastiano.indexino.cli.MainCommandKt"
        const val PROCESS_TIMEOUT_MINUTES = 3L
        const val MACOS_ARM64 = "macos-arm64"
        const val LINUX_X64 = "linux-x64"
        const val WINDOWS_X64 = "windows-x64"
    }
}
