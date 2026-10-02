package shell

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Проверки загрузки настоящих ZIP в память без распаковки. */
class ZipVfsTest {
    /** Пустой корректный архив содержит виртуальный корень. */
    @Test
    fun emptyZipLoadsAsAnEmptyRoot() {
        withArchive(emptyMap()) { path ->
            assertEquals(setOf("/"), ZipVfs.load(path).paths())
        }
    }

    /** Вложенные файлы создают промежуточные каталоги, пустые каталоги сохраняются. */
    @Test
    fun implicitAndEmptyDirectoriesArePreserved() {
        withArchive(mapOf("a/b/c/file.txt" to "hello".toByteArray(), "empty/" to byteArrayOf())) {
            val vfs = ZipVfs.load(it)
            assertEquals(setOf("/", "/a", "/a/b", "/a/b/c", "/a/b/c/file.txt", "/empty"),
                vfs.paths())
            assertContentEquals("hello".toByteArray(), vfs.read("/a/b/c/file.txt"))
        }
    }

    /** Нулевые и непечатные байты восстанавливаются без повреждения. */
    @Test
    fun binaryFilesRoundTripThroughBase64() {
        val bytes = byteArrayOf(0, 1, -1, -128, 127)
        withArchive(mapOf("binary.bin" to bytes)) { path ->
            assertContentEquals(bytes, ZipVfs.load(path).read("/binary.bin"))
        }
    }

    /** Обычная загрузка оставляет исходный архив и каталог неизменными. */
    @Test
    fun readingDoesNotModifyOrExtractTheArchive() {
        withArchive(mapOf("dir/file.txt" to "content".toByteArray())) { path ->
            val before = Files.readAllBytes(path)
            ZipVfs.load(path).read("/dir/file.txt")
            assertContentEquals(before, Files.readAllBytes(path))
            Files.list(path.parent).use { assertEquals(listOf(path), it.toList()) }
        }
    }

    /** Некорректное содержимое не считается пустым ZIP. */
    @Test
    fun malformedZipIsRejected() {
        withArchive(emptyMap()) { path ->
            Files.writeString(path, "not a ZIP")
            assertFailsWith<java.io.IOException> { ZipVfs.load(path) }
        }
    }

    /** Недопустимые пути и конфликт файла с каталогом отклоняются. */
    @Test
    fun unsafeAndConflictingPathsAreRejected() {
        val invalidNames = listOf("../outside", "/absolute", "a/../b", "a\\b", "a//b")
        invalidNames.forEach { name ->
            withArchive(mapOf(name to byteArrayOf())) { path ->
                assertFailsWith<IllegalArgumentException> { ZipVfs.load(path) }
            }
        }
        withArchive(mapOf("a" to byteArrayOf(), "a/b" to byteArrayOf())) { path ->
            assertFailsWith<IllegalArgumentException> { ZipVfs.load(path) }
        }
    }
}
