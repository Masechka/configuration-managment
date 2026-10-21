package shell

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверяет команды этапа 4 и изоляцию виртуальных путей от настоящего диска. */
class VfsCommandsTest {
    /** ls перечисляет только прямых детей и скрывает имена с точкой без -a. */
    @Test
    fun listDirectoriesFilesAndHiddenEntries() = withCommands { shell, _ ->
        assertEquals("a\nab\nroot.txt", shell.execute("ls").output)
        assertEquals(".\n..\n.secret\na\nab\nroot.txt", shell.execute("ls -a /").output)
        assertEquals("file.txt\nnested", shell.execute("ls /a").output)
        assertEquals("root.txt", shell.execute("ls root.txt").output)
        assertEquals("", shell.execute("ls ab").output)
        assertTrue(shell.execute("ls -l root.txt").output.contains("root root 4 root.txt"))
        assertTrue(shell.execute("ls -R a").output.contains("/a/nested:\nleaf.txt"))
        assertTrue(shell.execute("ls a ab").output.contains("/ab:"))
    }

    /** cd работает относительно текущего каталога, понимает .., . и cd -. */
    @Test
    fun navigateAndReturnToPreviousDirectory() = withCommands { shell, _ ->
        assertFalse(shell.execute("cd /a").error)
        assertEquals("file.txt\nnested", shell.execute("ls").output)
        assertFalse(shell.execute("cd nested/./").error)
        assertEquals("leaf.txt", shell.execute("ls").output)
        assertEquals("/a", shell.execute("cd -").output)
        assertFalse(shell.execute("cd ../ab/../../..").error)
        assertEquals("a\nab\nroot.txt", shell.execute("ls").output)
        shell.execute("cd /a")
        shell.execute("cd")
        assertEquals("a\nab\nroot.txt", shell.execute("ls").output)
    }

    /** Ошибки навигации не меняют каталог; нельзя пройти сквозь файл или пропажу. */
    @Test
    fun invalidPathsAndArgumentsPreserveDirectory() = withCommands { shell, _ ->
        assertTrue(shell.execute("cd -").error)
        shell.execute("cd a")
        val invalid = listOf(
            "cd /missing", "cd file.txt", "cd /root.txt/..", "cd /missing/../a",
            "cd /root.txt/", "cd /root.txt/.", "cd a b", "cd -x", "ls -z", "ls /no",
        )
        invalid.forEach { command -> assertTrue(shell.execute(command).error, command) }
        assertEquals("file.txt\nnested", shell.execute("ls").output)
        assertTrue(shell.execute("exit").output.contains(Shell.EXIT_WARNING))
    }

    /** Ошибочный cd сохраняет и текущий, и предыдущий каталог; cd - переключает их. */
    @Test
    fun failedNavigationPreservesPreviousDirectory() = withCommands { shell, _ ->
        shell.execute("cd a")
        shell.execute("cd nested")
        listOf("cd /missing", "cd /root.txt", "cd /a /ab", "cd -x").forEach { command ->
            assertTrue(shell.execute(command).error, command)
            assertEquals("leaf.txt", shell.execute("ls").output)
        }
        assertEquals("/a", shell.execute("cd -").output)
        assertEquals("/a/nested", shell.execute("cd -").output)
        assertFalse(shell.execute("cd ////a//nested/.././").error)
        assertEquals("file.txt\nnested", shell.execute("ls").output)
        assertFalse(shell.execute("cd ../../../../").error)
        assertEquals("a\nab\nroot.txt", shell.execute("ls").output)
        assertEquals("/a", shell.execute("cd -").output)
        assertFalse(shell.execute("cd --").error)
        assertEquals("/a", shell.execute("cd -").output)
    }

    /** -- позволяет перейти в каталог с дефисом; аргументы после него остаются путями. */
    @Test
    fun navigateToDirectoryStartingWithDash() {
        withArchive(mapOf("-folder/child/" to byteArrayOf(), "-folder/file" to byteArrayOf())) { path ->
            val shell = Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true)))
            assertTrue(shell.execute("cd -folder").error)
            assertFalse(shell.execute("cd -- -folder").error)
            assertEquals("child\nfile", shell.execute("ls").output)
            assertTrue(shell.execute("cd -- child extra").error)
            assertEquals("/", shell.execute("cd -").output)
            assertFalse(shell.execute("cd ./-folder/child/").error)
            assertEquals("", shell.execute("ls").output)
        }
    }

    /** find рекурсивно ищет имена и типы, не захватывая соседний префикс /ab. */
    @Test
    fun searchByNameTypeAndMultipleRoots() = withCommands { shell, _ ->
        assertEquals("a\na/file.txt\na/nested\na/nested/leaf.txt", shell.execute("find a").output)
        assertEquals("/a/file.txt\n/a/nested/leaf.txt",
            shell.execute("find /a -name *.txt -type f -print").output)
        assertEquals("/a\n/a/nested", shell.execute("find /a -type d").output)
        assertEquals("root.txt\nab", shell.execute("find root.txt ab").output)
        assertEquals("./.secret", shell.execute("find -name .secret").output)
        assertEquals("", shell.execute("find / -name no-match").output)
        assertEquals("/a/file.txt", shell.execute("find /a -name [f]ile.???").output)
        assertEquals("/a/nested/leaf.txt", shell.execute("find /a -name [!f]*.txt").output)
        assertEquals("/a/file.txt", shell.execute("find /a -name [a-f]*.txt").output)
        shell.execute("cd a/nested")
        assertEquals(".\n./leaf.txt", shell.execute("find").output)
    }

    /** Неподдерживаемые режимы и незавершённые предикаты дают диагностическую ошибку. */
    @Test
    fun findRejectsInvalidExpressions() = withCommands { shell, _ ->
        listOf("find /no", "find / -name", "find / -type x", "find / -type",
            "find / -exec", "find / -name [", "find / -type f extra").forEach { command ->
            assertTrue(shell.execute(command).error, command)
        }
    }

    /** Обычные команды не меняют ZIP; служебный сброс возвращает каталог в корень. */
    @Test
    fun readOnlyCommandsLeaveArchiveUnchangedAndResetClearsNavigation() = withCommands { shell, p ->
        val before = Files.readAllBytes(p)
        shell.execute("ls -alR /")
        shell.execute("find / -name *.txt")
        shell.execute("echo текст")
        shell.execute("cd /a/nested")
        assertContentEquals(before, Files.readAllBytes(p))
        assertFalse(shell.execute("vfs-init").error)
        assertEquals("", shell.execute("ls").output)
        assertEquals(".", shell.execute("find").output)
        assertTrue(shell.execute("cd -").error)
    }

    /** Создаёт дерево с вложенным и пустым каталогом, скрытым и двоичным файлом. */
    private fun withCommands(test: (Shell, java.nio.file.Path) -> Unit) {
        withArchive(mapOf(
            "a/file.txt" to "file".toByteArray(),
            "a/nested/leaf.txt" to byteArrayOf(0, 1, -1),
            "ab/" to byteArrayOf(),
            "root.txt" to "root".toByteArray(),
            ".secret" to byteArrayOf(),
        )) { path ->
            test(Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true))), path)
        }
    }
}
