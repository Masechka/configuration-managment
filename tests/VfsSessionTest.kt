package shell

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Проверки состояния сессии и физического сброса VFS. */
class VfsSessionTest {
    /** Служебная команда очищает память и физический ZIP, который снова читается. */
    @Test
    fun resetClearsMemoryAndPhysicalArchive() {
        withArchive(mapOf("old.txt" to "old".toByteArray())) { path ->
            val session = VfsSession.load(AppConfig(path, explicitVfs = true))
            assertTrue(session.fileSystem.paths().contains("/old.txt"))
            val result = Shell(session::initialize).execute("vfs-init")
            assertFalse(result.error)
            assertEquals(setOf("/"), session.fileSystem.paths())
            assertEquals(setOf("/"), ZipVfs.load(path).paths())
        }
    }

    /** Неверные аргументы не изменяют исходный архив. */
    @Test
    fun resetRejectsArgumentsWithoutWriting() {
        withArchive(mapOf("old.txt" to "old".toByteArray())) { path ->
            val before = Files.readAllBytes(path)
            val session = VfsSession.load(AppConfig(path, explicitVfs = true))
            assertTrue(Shell(session::initialize).execute("vfs-init extra").error)
            assertContentEquals(before, Files.readAllBytes(path))
        }
    }

    /** Ошибка записи сохраняет старую файловую систему в памяти. */
    @Test
    fun failedResetPreservesMemoryAndReportsAnError() {
        withArchive(mapOf("old.txt" to "old".toByteArray())) { path ->
            val session = VfsSession.load(AppConfig(path, explicitVfs = true))
            Files.delete(path)
            Files.createDirectory(path)
            Files.writeString(path.resolve("blocker"), "do not overwrite")
            val result = Shell(session::initialize).execute("vfs-init")
            assertTrue(result.error)
            assertContentEquals("old".toByteArray(), session.fileSystem.read("/old.txt"))
            assertEquals("do not overwrite", Files.readString(path.resolve("blocker")))
        }
    }

    /** Неявный отсутствующий ZIP допускает пустую VFS без записи на диск. */
    @Test
    fun absentDefaultStaysInMemoryUntilReset() {
        withArchive(emptyMap()) { path ->
            Files.delete(path)
            val session = VfsSession.load(AppConfig(path))
            assertFalse(Files.exists(path))
            assertEquals(setOf("/"), session.fileSystem.paths())
            session.initialize()
            assertEquals(setOf("/"), ZipVfs.load(path).paths())
        }
    }

    /** Явно заданный отсутствующий архив сообщается как ошибка. */
    @Test
    fun explicitlyMissingZipIsRejected() {
        withArchive(emptyMap()) { path ->
            Files.delete(path)
            assertFailsWith<java.io.IOException> {
                VfsSession.load(AppConfig(path, explicitVfs = true))
            }
        }
    }
}
