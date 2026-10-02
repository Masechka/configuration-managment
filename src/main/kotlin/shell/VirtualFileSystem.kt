package shell

import java.util.Base64

/** Неизменяемое дерево VFS; содержимое файлов хранится в памяти в base64. */
class VirtualFileSystem internal constructor(
    files: Map<String, String> = emptyMap(),
    directories: Set<String> = setOf("/"),
) {
    private val files = files.toMap()
    private val directories = directories.toSet()

    /** Количество файлов в загруженной VFS. */
    val fileCount: Int get() = files.size

    /** Количество каталогов, включая виртуальный корень. */
    val directoryCount: Int get() = directories.size

    /** Возвращает независимый список абсолютных виртуальных путей. */
    fun paths(): Set<String> = (directories + files.keys).toSortedSet()

    /** Восстанавливает исходные байты файла, не обращаясь к диску. */
    fun read(path: String): ByteArray {
        val encoded = files[path] ?: throw IllegalArgumentException("Файл VFS не найден: $path")
        return Base64.getDecoder().decode(encoded)
    }

    /** Диагностика загрузки позволяет увидеть дерево до реализации ls на этапе 4. */
    fun describe(): String {
        val tree = paths().joinToString("\n") { path ->
            if (path in directories && path != "/") "$path/" else path
        }
        return "VFS: файлов=$fileCount, каталогов=$directoryCount\n$tree"
    }
}
