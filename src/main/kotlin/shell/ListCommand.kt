package shell

/** Перечисляет файлы VFS; рекурсия не затрагивает диск или скрытые поддеревья без -a. */
internal class ListCommand(private val navigation: VfsNavigation) {
    private val fileSystem: VirtualFileSystem get() = navigation.fileSystem

    /** Поддерживает файлы, каталоги, несколько путей, -a, -l, -R и --. */
    fun execute(arguments: List<String>): String {
        val options = CommandOptions.parse(arguments, "alR")
        val paths = options.operands.ifEmpty { listOf(".") }.map(navigation::resolve)
        return paths.joinToString("\n\n") { path ->
            if (fileSystem.isDirectory(path)) {
                listDirectory(path, options, paths.size > 1)
            } else {
                format(path, path.substringAfterLast('/'), options)
            }
        }
    }

    /** Перечисляет каталоги итеративно, чтобы глубина VFS не ограничивалась стеком. */
    private fun listDirectory(path: String, options: CommandOptions, multiple: Boolean): String {
        val pending = ArrayDeque<String>()
        val sections = mutableListOf<String>()
        pending.add(path)
        while (pending.isNotEmpty()) {
            val directory = pending.removeLast()
            val children = visibleChildren(directory, options)
            val rows = dotEntries(directory, options) + children.map { child ->
                format(child, child.substringAfterLast('/'), options)
            }
            val header = if (multiple || 'R' in options.flags) "$directory:\n" else ""
            sections.add(header + rows.joinToString("\n"))
            if ('R' in options.flags) {
                children.filter(fileSystem::isDirectory).reversed().forEach(pending::addLast)
            }
        }
        return sections.joinToString("\n\n")
    }

    /** ls без -a пропускает только прямых детей с именем, начинающимся с точки. */
    private fun visibleChildren(path: String, options: CommandOptions): List<String> {
        return fileSystem.children(path).filter {
            'a' in options.flags || !it.substringAfterLast('/').startsWith('.')
        }
    }

    /** -a показывает виртуальные . и .., не включая их в рекурсивный обход. */
    private fun dotEntries(path: String, options: CommandOptions): List<String> {
        if ('a' !in options.flags) return emptyList()
        val parent = path.substringBeforeLast('/').ifEmpty { "/" }
        return listOf(format(path, ".", options), format(parent, "..", options))
    }

    /** Длинный формат: тип, виртуальный владелец, группа, размер в байтах и имя. */
    private fun format(path: String, name: String, options: CommandOptions): String {
        if ('l' !in options.flags) return name
        val type = if (fileSystem.isDirectory(path)) "d" else "-"
        return "$type root root ${fileSystem.size(path)} $name"
    }
}
