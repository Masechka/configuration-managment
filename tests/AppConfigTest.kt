package shell

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверки настройки путей и ошибок параметров запуска. */
class AppConfigTest {
    /** Значения по умолчанию позволяют запустить прототип без параметров. */
    @Test
    fun defaultsDoNotRequireAStartupScript() {
        val config = AppConfig.parse(emptyArray())
        assertEquals(Path.of("default.zip"), config.vfsPath)
        assertEquals(null, config.startupPath)
        assertFalse(config.explicitVfs)
    }

    /** Параметры принимаются в любом порядке и сохраняют пробелы в пути. */
    @Test
    fun bothPathsPreserveSpacesAndDetermineVfsName() {
        val config = AppConfig.parse(
            arrayOf("--startup", "my scripts/start.txt", "--vfs", "my vfs/demo.zip"),
        )
        assertEquals(Path.of("my scripts/start.txt"), config.startupPath)
        assertEquals(Path.of("my vfs/demo.zip"), config.vfsPath)
        assertEquals("demo.zip", config.vfsName)
        assertTrue(config.explicitVfs)
        assertTrue(config.debugDescription().contains("my scripts/start.txt"))
        assertTrue(config.debugDescription().contains("my vfs/demo.zip"))
    }

    /** Оба параметра могут использоваться независимо. */
    @Test
    fun eachParameterCanBeUsedAlone() {
        assertEquals(Path.of("a.zip"), AppConfig.parse(arrayOf("--vfs", "a.zip")).vfsPath)
        val config = AppConfig.parse(arrayOf("--startup", "a.txt"))
        assertEquals(Path.of("a.txt"), config.startupPath)
        assertFalse(config.explicitVfs)
    }

    /** Неизвестные параметры, повторы и пропущенные значения отклоняются. */
    @Test
    fun malformedArgumentsAreRejected() {
        val invalid = listOf(
            arrayOf("--unknown", "file"),
            arrayOf("--vfs"),
            arrayOf("--startup"),
            arrayOf("--vfs", "--startup", "a.txt"),
            arrayOf("--vfs", "a.zip", "--vfs", "b.zip"),
            arrayOf("--startup", "a.txt", "--startup", "b.txt"),
            arrayOf("--vfs", ""),
        )
        invalid.forEach { args ->
            assertFailsWith<IllegalArgumentException> { AppConfig.parse(args) }
        }
    }
}
