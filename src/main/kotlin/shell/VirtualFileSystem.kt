package shell

import java.util.Base64

/** Дерево VFS с неизменяемым содержимым base64 и изменяемыми владельцами в памяти. */
class VirtualFileSystem internal constructor(
    files: Map<String, String> = emptyMap(),
    directories: Set<String> = setOf("/"),
) {
    private val files = files.toMap()
    private val directories = directories.toSet()
    private val ownership = mutableMapOf<String, VfsOwnership>()

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

    /** Проверяет существование файла или каталога по абсолютному пути. */
    fun contains(path: String): Boolean = path in directories || path in files

    /** Отличает каталоги от обычных файлов. */
    fun isDirectory(path: String): Boolean = path in directories

    /** Возвращает прямых детей каталога в стабильном порядке. */
    fun children(path: String): List<String> {
        val prefix = if (path == "/") "/" else "$path/"
        return paths().filter { candidate ->
            candidate != path && candidate.startsWith(prefix) &&
                '/' !in candidate.removePrefix(prefix)
        }
    }

    /** Возвращает путь и его потомков; соседние префиксы не попадают в поддерево. */
    fun subtree(path: String): List<String> {
        val prefix = if (path == "/") "/" else "$path/"
        return paths().filter { it == path || it.startsWith(prefix) }
    }

    /** Размер файла в исходных байтах; каталог имеет виртуальный размер 0. */
    fun size(path: String): Int = if (isDirectory(path)) 0 else read(path).size

    /** Возвращает метаданные существующего элемента; начальный владелец — root:root. */
    fun ownership(path: String): VfsOwnership {
        require(contains(path)) { "Путь VFS не найден: $path" }
        return ownership[path] ?: VfsOwnership()
    }

    /** Меняет только заданные поля метаданных существующего элемента в памяти. */
    fun changeOwnership(path: String, owner: String?, group: String?) {
        require(owner != null || group != null) { "Не задан владелец или группа" }
        val previous = ownership(path)
        ownership[path] = VfsOwnership(owner ?: previous.owner, group ?: previous.group)
    }

    /** Диагностика загруженного дерева без распаковки на диск. */
    fun describe(): String {
        val tree = paths().joinToString("\n") { path ->
            if (path in directories && path != "/") "$path/" else path
        }
        return "VFS: файлов=$fileCount, каталогов=$directoryCount\n$tree"
    }
}
