package shell

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверяет смену виртуального владельца и отсутствие физических изменений ZIP. */
class OwnershipTest {
    /** chown меняет владельца файла, а ls -l показывает изменение и исходный размер. */
    @Test
    fun changeOwnerOfFileAndDirectory() = withOwnership { shell, _, _ ->
        assertFalse(shell.execute("chown alice /a/file.txt").error)
        assertEquals("- alice root 4 file.txt", shell.execute("ls -l /a/file.txt").output)
        assertFalse(shell.execute("chown bob:staff a").error)
        assertEquals("d bob staff 0 a", shell.execute("ls -l /").output.lines().first())
        assertEquals("- alice root 4 file.txt", shell.execute("ls -l a/file.txt").output)
    }

    /** Можно сменить только группу, числовые IDs или виртуальную группу по имени владельца. */
    @Test
    fun groupOnlyNumericIdsAndDefaultGroup() = withOwnership { shell, _, _ ->
        shell.execute("chown alice:staff root.txt")
        assertFalse(shell.execute("chown :team root.txt").error)
        assertEquals("- alice team 4 root.txt", shell.execute("ls -l root.txt").output)
        assertFalse(shell.execute("chown 1000:2000 root.txt").error)
        assertEquals("- 1000 2000 4 root.txt", shell.execute("ls -l root.txt").output)
        assertFalse(shell.execute("chown student: root.txt").error)
        assertEquals("- student student 4 root.txt", shell.execute("ls -l root.txt").output)
    }

    /** Рекурсия включает каталог, вложенные и скрытые элементы, но не соседний /ab. */
    @Test
    fun recursiveOwnershipAndMultipleRelativePaths() = withOwnership { shell, _, _ ->
        shell.execute("cd a")
        assertFalse(shell.execute("chown -R alice:team . ../root.txt").error)
        assertEquals("- alice team 4 file.txt", shell.execute("ls -l file.txt").output)
        assertEquals("- alice team 3 leaf.txt", shell.execute("ls -l nested/leaf.txt").output)
        assertEquals("- alice team 0 .hidden", shell.execute("ls -l .hidden").output)
        assertEquals("- alice team 4 root.txt", shell.execute("ls -l ../root.txt").output)
        assertEquals("d root root 0 ab", shell.execute("ls -l ..").output.lines()[1])
        assertTrue(shell.execute("ls -al .").output.lines()[0].contains("alice team"))
    }

    /** Ошибки аргументов и путей не меняют метаданные уже существующих элементов. */
    @Test
    fun invalidCommandsDoNotChangeOwners() = withOwnership { shell, _, _ ->
        val invalid = listOf(
            "chown", "chown alice", "chown : root.txt", "chown a:b:c root.txt",
            "chown -x alice root.txt", "chown alice root.txt /missing",
            "chown alice /root.txt/..", "chown alice /missing/../a", "chown bad/name root.txt",
        )
        invalid.forEach { command -> assertTrue(shell.execute(command).error, command) }
        assertEquals("- root root 4 root.txt", shell.execute("ls -l root.txt").output)
        assertTrue(shell.execute("exit").output.contains(Shell.EXIT_WARNING))
    }

    /** Метаданные и содержимое остаются в памяти; новая загрузка не видит изменений владельцев. */
    @Test
    fun ownershipDoesNotWriteZipOrModifyFileBytes() = withOwnership { shell, session, path ->
        val before = Files.readAllBytes(path)
        assertFalse(shell.execute("chown -R alice:staff /").error)
        assertContentEquals(byteArrayOf(0, 1, -1), session.fileSystem.read("/a/nested/leaf.txt"))
        assertContentEquals(before, Files.readAllBytes(path))
        val reloaded = Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true)))
        assertEquals("- root root 4 root.txt", reloaded.execute("ls -l root.txt").output)
        assertTrue(shell.execute("ls -al /").output.lines()[0].contains("alice staff"))
    }

    /** Служебный сброс убирает владельцев старого дерева, возвращая новый корень root:root. */
    @Test
    fun resetClearsOwnershipAndNavigation() = withOwnership { shell, _, _ ->
        shell.execute("chown -R alice:staff /")
        shell.execute("cd a/nested")
        assertFalse(shell.execute("vfs-init").error)
        assertEquals("d root root 0 .\nd root root 0 ..", shell.execute("ls -al").output)
        assertTrue(shell.execute("chown alice /root.txt").error)
        assertTrue(shell.execute("cd -").error)
    }

    /** Ошибка служебной записи сохраняет владельца и текущий каталог в памяти. */
    @Test
    fun failedResetPreservesOwnershipAndNavigation() = withOwnership { shell, _, path ->
        shell.execute("chown alice a/file.txt")
        shell.execute("cd a")
        Files.delete(path)
        Files.createDirectory(path)
        Files.writeString(path.resolve("blocker"), "keep")
        assertTrue(shell.execute("vfs-init").error)
        assertEquals("- alice root 4 file.txt", shell.execute("ls -l file.txt").output)
    }

    /** -- позволяет передать путь, начинающийся с дефиса, без трактовки как опции. */
    @Test
    fun doubleDashHandlesDashFilename() {
        withArchive(mapOf("-file" to byteArrayOf())) { path ->
            val shell = Shell(session = VfsSession.load(AppConfig(path, explicitVfs = true)))
            assertFalse(shell.execute("chown -- alice -file").error)
            assertEquals("- alice root 0 -file", shell.execute("ls -l -- -file").output)
        }
    }

    /** Оболочка без загруженного архива хранит владельца пустого корня между командами. */
    @Test
    fun fallbackShellKeepsRootOwnership() {
        val shell = Shell()
        assertFalse(shell.execute("chown alice:team /").error)
        assertEquals("d alice team 0 .\nd alice team 0 ..", shell.execute("ls -al /").output)
    }

    /** Создаёт VFS с соседним префиксом, скрытым файлом и двоичными данными. */
    private fun withOwnership(test: (Shell, VfsSession, Path) -> Unit) {
        withArchive(mapOf(
            "a/file.txt" to "file".toByteArray(),
            "a/nested/leaf.txt" to byteArrayOf(0, 1, -1),
            "a/.hidden" to byteArrayOf(),
            "ab/" to byteArrayOf(),
            "root.txt" to "root".toByteArray(),
        )) { path ->
            val session = VfsSession.load(AppConfig(path, explicitVfs = true))
            test(Shell(session = session), session, path)
        }
    }
}
