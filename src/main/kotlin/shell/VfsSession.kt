package shell

import java.nio.file.Files
import java.nio.file.Path

/** Связывает текущую VFS в памяти с единственным выбранным физическим архивом. */
class VfsSession private constructor(
    private val path: Path,
    fileSystem: VirtualFileSystem,
) {
    /** Выбранное дерево; заменяется только после успешной физической очистки. */
    var fileSystem: VirtualFileSystem = fileSystem
        private set

    /** Навигация всегда обращается к актуальному дереву этой сессии. */
    val navigation = VfsNavigation { this.fileSystem }

    /** Сбрасывает ZIP и память; при ошибке записи прежнее дерево сохраняется. */
    fun initialize(): String {
        ZipVfs.reset(path)
        fileSystem = VirtualFileSystem()
        navigation.reset()
        return "VFS сброшена: $path\n${fileSystem.describe()}"
    }

    /** Создание сессии по настройкам приложения. */
    companion object {
        /** Неявная отсутствующая VFS остаётся в памяти, явный путь должен существовать. */
        fun load(config: AppConfig): VfsSession {
            val fileSystem = if (!config.explicitVfs && !Files.exists(config.vfsPath)) {
                VirtualFileSystem()
            } else {
                ZipVfs.load(config.vfsPath)
            }
            return VfsSession(config.vfsPath, fileSystem)
        }
    }
}
