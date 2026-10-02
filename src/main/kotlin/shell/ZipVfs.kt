package shell

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Чтение ZIP в память и разрешённая заданием очистка его физического представления. */
object ZipVfs {
    /** Читает корректный ZIP; исходный архив не изменяется и не распаковывается. */
    fun load(path: Path): VirtualFileSystem {
        val builder = ZipTreeBuilder()
        ZipFile(path.toFile()).use { zip ->
            zip.entries().asSequence().forEach { entry -> builder.add(zip, entry) }
        }
        return builder.build()
    }

    /** Заменяет выбранный архив пустым корректным ZIP через временный файл. */
    fun reset(path: Path) {
        val target = path.toAbsolutePath()
        val temporary = Files.createTempFile(target.parent, ".vfs-reset-", ".zip")
        try {
            ZipOutputStream(Files.newOutputStream(temporary)).use { }
            replace(temporary, target)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    /** Предпочитает атомарную замену, поддерживает файловые системы без неё. */
    private fun replace(source: Path, target: Path) {
        try {
            Files.move(source, target, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, REPLACE_EXISTING)
        }
    }
}
