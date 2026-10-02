package shell

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Создаёт демонстрационные архивы из текстовых исходников, без бинарных файлов в Git. */
object ExampleArchives {
    /** Генерирует минимальный архив, несколько файлов и дерево из трёх уровней. */
    fun create(directory: Path) {
        Files.createDirectories(directory)
        write(directory.resolve("minimal.zip"), emptyMap())
        write(directory.resolve("several.zip"), mapOf(
            "hello.txt" to "Привет из VFS!\n".toByteArray(Charsets.UTF_8),
            "notes.txt" to "Несколько файлов.\n".toByteArray(Charsets.UTF_8),
            "binary.bin" to byteArrayOf(0, 1, -1, 127),
        ))
        write(directory.resolve("deep.zip"), mapOf(
            "home/" to byteArrayOf(),
            "home/student/" to byteArrayOf(),
            "home/student/projects/" to byteArrayOf(),
            "home/student/projects/readme.txt" to "Три уровня каталогов.\n".toByteArray(),
            "home/student/profile.txt" to "Студент\n".toByteArray(),
            "root.txt" to "Корень\n".toByteArray(),
            "empty/" to byteArrayOf(),
        ))
        println("Примеры ZIP созданы: $directory")
    }

    /** Сохраняет один тестовый архив, отделённый от пользовательской VFS. */
    private fun write(path: Path, entries: Map<String, ByteArray>) {
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
    }
}

/** Точка входа генератора; не является командой эмулятора. */
fun main(args: Array<String>) {
    require(args.size == GENERATOR_ARGUMENT_COUNT) { "Ожидается каталог для примеров ZIP" }
    ExampleArchives.create(Path.of(args.single()))
}

private const val GENERATOR_ARGUMENT_COUNT = 1
