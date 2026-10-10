package shell

/** Разрешает пути только в VFS и хранит виртуальный текущий каталог. */
class VfsNavigation(private val source: () -> VirtualFileSystem) {
    /** Текущее дерево, включая замену после vfs-init. */
    val fileSystem: VirtualFileSystem get() = source()

    /** Абсолютный путь текущего виртуального каталога. */
    var currentDirectory: String = "/"
        private set
    private var previousDirectory: String? = null

    /** Разрешает . и .., проверяя каждый промежуточный каталог до перехода. */
    fun resolve(argument: String): String {
        require(argument.isNotEmpty()) { "Пустой путь VFS" }
        var path = if (argument.startsWith('/')) "/" else currentDirectory
        argument.split('/').filter { it.isNotEmpty() }.forEach { part ->
            requireDirectory(path)
            path = when (part) {
                "." -> path
                ".." -> parent(path)
                else -> if (path == "/") "/$part" else "$path/$part"
            }
            require(fileSystem.contains(path)) { "Путь VFS не найден: $path" }
        }
        if (argument.endsWith('/')) requireDirectory(path)
        return path
    }

    /** cd без аргумента возвращает в корень; cd - возвращает предыдущий каталог. */
    fun changeDirectory(argument: String?): String {
        val target = when (argument) {
            null -> "/"
            "-" -> previousDirectory ?: errorNoPrevious()
            else -> resolve(argument)
        }
        requireDirectory(target)
        previousDirectory = currentDirectory
        currentDirectory = target
        return if (argument == "-") target else ""
    }

    /** Сбрасывает навигацию вместе с заменой дерева VFS. */
    fun reset() {
        currentDirectory = "/"
        previousDirectory = null
    }

    /** Проверяет тип уже разрешённого пути. */
    private fun requireDirectory(path: String) {
        require(fileSystem.isDirectory(path)) { "Не каталог VFS: $path" }
    }

    /** Не допускает выхода за виртуальный корень. */
    private fun parent(path: String): String = path.substringBeforeLast('/').ifEmpty { "/" }

    /** Отсутствие предыдущего каталога является ошибкой команды, а не приложения. */
    private fun errorNoPrevious(): Nothing {
        throw IllegalArgumentException("Предыдущий каталог VFS не задан")
    }
}
