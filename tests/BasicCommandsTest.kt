package shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверки основных команд на настоящем архиве без обращения команд к диску. */
class BasicCommandsTest {
    /** echo соединяет аргументы, сохраняя простой парсер из этапа 1. */
    @Test
    fun echoPrintsTextWithoutExpansion() {
        assertEquals("Привет мир", Shell().execute("echo  Привет   мир").output)
        assertEquals("", Shell().execute("echo").output)
        assertEquals("\"текст\" \$HOME -n", Shell().execute("echo \"текст\" \$HOME -n").output)
    }

    /** Пустая VFS содержит только корень; команды больше не являются заглушками. */
    @Test
    fun emptyRootHasRealNavigationAndSearch() {
        val shell = Shell()
        assertEquals("", shell.execute("ls").output)
        assertEquals("", shell.execute("cd /").output)
        assertEquals(".", shell.execute("find").output)
        assertTrue(shell.execute("cd /missing").error)
        assertTrue(shell.execute("ls /missing").error)
    }
    /** Шаблон имени сопоставляется со строкой /, а не с разделителем настоящего пути. */
    @Test
    fun findNameMatchesVirtualRoot() {
        val shell = Shell()
        listOf("*", "?", "[!a]", "[/]", "/").forEach { pattern ->
            val result = shell.execute("find / -name $pattern")
            assertFalse(result.error, pattern)
            assertEquals("/", result.output, pattern)
        }
    }

    /** Glob работает с любым символом имени ZIP, включая перевод строки. */
    @Test
    fun wildcardMatchesMultilineFilename() {
        withArchive(mapOf("a\nb.txt" to byteArrayOf())) { path ->
            val shell = Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true)))
            assertEquals("/a\nb.txt", shell.execute("find / -type f -name *.txt").output)
        }
    }

}
