package nuget

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Проверяет все пять настроек CLI и неверный ввод. */
class CliConfigTest {
    /** Пропущенные необязательные параметры получают документированные значения. */
    @Test fun defaults() {
        val config = Config.parse(arrayOf("--package", "Newtonsoft.Json"))
        assertEquals("Newtonsoft.Json", config.packageName)
        assertEquals("https://api.nuget.org/v3/index.json", config.repository)
        assertEquals(false, config.testMode)
        assertEquals(false, config.ascii)
        assertEquals(3, config.maxDepth)
        assertEquals(5, config.describe().size)
    }

    /** Значения задаются только опциями командной строки. */
    @Test fun explicitValues() {
        val path = Files.createTempFile("graph", ".txt")
        val config = Config.parse(arrayOf(
            "--package", "A", "--repository", path.toString(), "--test-mode", "true",
            "--ascii", "true", "--max-depth", "0"
        ))
        assertTrue(config.testMode)
        assertTrue(config.ascii)
        assertEquals(0, config.maxDepth)
        assertEquals(path.toString(), config.repository)
        Files.delete(path)
    }

    /** Ошибки имени, URL, флагов, глубины и структуры аргументов отклоняются. */
    @Test fun invalidArguments() {
        val cases = listOf(
            emptyArray(), arrayOf("--package", ""), arrayOf("--package", "bad/name"),
            arrayOf("--package", "A", "--repository", "ftp://host/index.json"),
            arrayOf("--package", "A", "--repository", "https:///missing"),
            arrayOf("--package", "A", "--test-mode", "yes"),
            arrayOf("--package", "A", "--ascii", "1"),
            arrayOf("--package", "A", "--max-depth", "-1"),
            arrayOf("--package", "A", "--max-depth", "word"),
            arrayOf("--package", "A", "--max-depth", "2147483648"),
            arrayOf("--package", "A", "--unknown", "value"),
            arrayOf("--package", "A", "--package", "B"), arrayOf("--package"),
            arrayOf("--package", "a", "--test-mode", "true", "--repository", "graph.txt"),
            arrayOf("--package", "A", "--test-mode", "true")
        )
        cases.forEach { args ->
            assertFailsWith<IllegalArgumentException>(args.toList().toString()) {
                Config.parse(args)
            }
        }
    }
}
