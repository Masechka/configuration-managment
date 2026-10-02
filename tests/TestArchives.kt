package shell

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Создаёт ZIP только во временной папке, не сохраняя бинарные фикстуры в Git. */
fun withArchive(entries: Map<String, ByteArray>, test: (Path) -> Unit) {
    val directory = Files.createTempDirectory("vfs test with spaces")
    val path = directory.resolve("sample.zip")
    try {
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        test(path)
    } finally {
        Files.walk(directory).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
