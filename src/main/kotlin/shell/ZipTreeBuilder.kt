package shell

import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Строит дерево VFS из записей ZIP и проверяет конфликты виртуальных путей. */
internal class ZipTreeBuilder {
    private val files = mutableMapOf<String, String>()
    private val directories = mutableSetOf("/")
    private val seen = mutableSetOf<String>()

    /** Добавляет каталог или файл без распаковки на диск. */
    fun add(zip: ZipFile, entry: ZipEntry) {
        val parts = checkedSegments(entry)
        val path = "/${parts.joinToString("/")}"
        require(seen.add(path)) { "Повтор пути в ZIP: $path" }
        val parentParts = if (entry.isDirectory) parts else parts.dropLast(1)
        addDirectories(parentParts)
        if (entry.isDirectory) return
        require(path !in directories) { "Файл совпадает с каталогом: $path" }
        val bytes = zip.getInputStream(entry).use { it.readBytes() }
        val checksum = CRC32().apply { update(bytes) }
        require(checksum.value == entry.crc) { "Повреждён файл в ZIP: $path" }
        files[path] = Base64.getEncoder().encodeToString(bytes)
    }

    /** Завершает загрузку, отделяя полученную VFS от изменяемого построителя. */
    fun build(): VirtualFileSystem = VirtualFileSystem(files, directories)

    /** Проверяет, что запись задаёт однозначный путь внутри виртуального корня. */
    private fun checkedSegments(entry: ZipEntry): List<String> {
        val name = entry.name.removeSuffix("/")
        require(name.isNotEmpty() && !name.startsWith("/") && '\\' !in name) {
            "Некорректный путь в ZIP: ${entry.name}"
        }
        val parts = name.split('/')
        require(parts.none { it.isEmpty() || it == "." || it == ".." }) {
            "Некорректный путь в ZIP: ${entry.name}"
        }
        return parts
    }

    /** Восстанавливает отсутствующие в ZIP промежуточные каталоги. */
    private fun addDirectories(parts: List<String>) {
        var path = ""
        parts.forEach { part ->
            path += "/$part"
            require(path !in files) { "Каталог совпадает с файлом: $path" }
            directories.add(path)
        }
    }
}
