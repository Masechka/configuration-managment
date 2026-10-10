package shell

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Исполняет демонстрационные сценарии, которые пользователь запускает на защите. */
class Stage4DemoTest {
    /** Все нормальные строки работают; ровно десять ошибочных строк не прерывают диалог. */
    @Test
    fun interactiveScenarioRunsAllModesWithoutChangingZip() {
        withDemo("stage4.txt") { results, dialogue, before, after ->
            assertEquals(10, results.count { it.error })
            assertFalse(results.any { it.exit })
            assertTrue(dialogue.contains("После ошибок команды продолжают выполняться"))
            assertTrue(dialogue.endsWith("readme.txt"))
            assertTrue(dialogue.contains("/.hidden/secret.txt"))
            assertContentEquals(before, after)
        }
    }

    /** exit предупреждает о прошлых ошибках и не выполняет следующую строку. */
    @Test
    fun exitScenarioStopsAndWarnsAfterErrors() {
        withDemo("stage4-exit.txt") { results, dialogue, before, after ->
            assertTrue(results.last().exit)
            assertTrue(dialogue.contains(Shell.EXIT_WARNING))
            assertFalse(dialogue.contains("unknown-must-not-run"))
            assertContentEquals(before, after)
        }
    }

    /** Генерирует образцы только во временной папке и запускает реальный StartupScript. */
    private fun withDemo(
        name: String,
        test: (List<CommandResult>, String, ByteArray, ByteArray) -> Unit,
    ) {
        val directory = Files.createTempDirectory("stage4-demo")
        try {
            ExampleArchives.create(directory)
            val path = directory.resolve("deep.zip")
            val before = Files.readAllBytes(path)
            val shell = Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true)))
            val results = mutableListOf<CommandResult>()
            val output = mutableListOf<String>()
            StartupScript.run(Path.of("examples/startup/$name"), { line ->
                shell.execute(line).also(results::add)
            }, output::add)
            test(results, output.joinToString("\n"), before, Files.readAllBytes(path))
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }
}
