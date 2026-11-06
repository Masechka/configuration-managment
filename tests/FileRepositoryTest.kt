package nuget

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Проверяет описание тестового графа в UTF-8 файле. */
class FileRepositoryTest {
    /** Комментарии, пустые строки, лист, цикл и повтор зависимости допустимы. */
    @Test fun readsFile() {
        val path = Files.createTempFile("repository", ".txt")
        try {
            Files.writeString(path, "# graph\n\nA: B B\nB: A\nEMPTY:")
            val repository = FileRepository.read(path)
            assertEquals(listOf(Dependency("B")), repository.dependencies("A"))
            assertEquals(emptyList(), repository.dependencies("EMPTY"))
            assertFailsWith<IllegalArgumentException> { repository.dependencies("MISSING") }
        } finally { Files.delete(path) }
    }

    /** Повреждённый граф не исправляется молчаливым добавлением узлов. */
    @Test fun rejectsInvalidGraphs() {
        listOf("", "a:", "A B", "A: b", "A: B", "A:\nA:", "A: B:C").forEach {
            assertFailsWith<IllegalArgumentException>(it) { FileRepository.fromText(it) }
        }
    }
}
