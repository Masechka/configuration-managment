package shell

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверки демонстраций этапа 5 и изменений VFS только в памяти. */
class Stage5DemoTest {
    /** Все режимы chown меняют метаданные; десять ошибок не прерывают диалог. */
    @Test
    fun interactiveScenarioChangesOnlyMemory() =
        withScenario("stage5.txt") { shell, path, results ->
            assertEquals(10, results.count { it.error })
            assertFalse(results.any { it.exit })
            assertEquals("- final group 13 root.txt", shell.execute("ls -l /root.txt").output)
            val reloaded = Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true)))
            assertEquals("- root root 13 root.txt", reloaded.execute("ls -l /root.txt").output)
        }
    /** exit-сценарий сбрасывает дерево и владельцев, затем предупреждает и завершает диалог. */
    @Test
    fun exitScenarioResetsAndStops() = withScenario("stage5-exit.txt") { shell, path, results ->
        assertEquals(10, results.count { it.error })
        assertTrue(results.last().exit)
        assertTrue(results.last().output.contains(Shell.EXIT_WARNING))
        assertEquals("d root root 0 .\nd root root 0 ..", shell.execute("ls -al").output)
        assertEquals(setOf("/"), ZipVfs.load(path).paths())
    }

    /** Исполняет сохранённый сценарий на генерируемом ZIP с контролем каждого результата. */
    private fun withScenario(
        name: String,
        test: (Shell, Path, List<CommandResult>) -> Unit,
    ) {
        val directory = Files.createTempDirectory("stage5-demo")
        try {
            ExampleArchives.create(directory)
            val path = directory.resolve("deep.zip")
            val before = Files.readAllBytes(path)
            val shell = Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true)))
            val results = mutableListOf<CommandResult>()
            StartupScript.run(Path.of("examples/startup/$name"), { line ->
                shell.execute(line).also(results::add)
            }, {})
            if (name == "stage5.txt") assertContentEquals(before, Files.readAllBytes(path))
            test(shell, path, results)
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }
}
