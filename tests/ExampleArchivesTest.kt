package shell

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Проверки генератора демонстрационных архивов. */
class ExampleArchivesTest {
    /** Три примера включают пустую VFS, несколько файлов и глубокое дерево. */
    @Test
    fun examplesCoverRequiredVfsShapes() {
        val directory = Files.createTempDirectory("example archives")
        try {
            ExampleArchives.create(directory)
            assertEquals(setOf("/"), ZipVfs.load(directory.resolve("minimal.zip")).paths())
            val several = ZipVfs.load(directory.resolve("several.zip"))
            assertTrue(several.paths().containsAll(listOf("/hello.txt", "/binary.bin")))
            assertContentEquals(byteArrayOf(0, 1, -1, 127), several.read("/binary.bin"))
            val deep = ZipVfs.load(directory.resolve("deep.zip"))
            assertTrue(deep.paths().contains("/home/student/projects/readme.txt"))
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }
}
