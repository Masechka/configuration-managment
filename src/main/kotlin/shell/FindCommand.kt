package shell


/** Рекурсивно ищет виртуальные пути по имени и типу без доступа к содержимому диска. */
internal class FindCommand(private val navigation: VfsNavigation) {
    /** Обходит указанные корни; отсутствие пути означает текущий каталог. */
    fun execute(arguments: List<String>): String {
        val roots = arguments.takeWhile { !it.startsWith('-') }.ifEmpty { listOf(".") }
        val expression = arguments.dropWhile { !it.startsWith('-') }
        val predicates = parseExpression(expression)
        return roots.flatMap { root ->
            val absolute = navigation.resolve(root)
            navigation.fileSystem.subtree(absolute).filter { path ->
                predicates.all { it(path) }
            }.map { path -> displayPath(root, absolute, path) }
        }.joinToString("\n")
    }

    /** Предикаты объединяются логическим И; -print обозначает обычный вывод путей. */
    private fun parseExpression(arguments: List<String>): List<(String) -> Boolean> {
        val predicates = mutableListOf<(String) -> Boolean>()
        var index = 0
        while (index < arguments.size) {
            val option = arguments[index++]
            if (option == "-print") continue
            require(option == "-name" || option == "-type") {
                "find: неподдерживаемый предикат: $option"
            }
            require(index < arguments.size) { "find: ожидается значение после $option" }
            val value = arguments[index++]
            predicates.add(if (option == "-name") namePredicate(value) else typePredicate(value))
        }
        return predicates
    }

    /** glob поддерживает *, ?, классы [abc] и [!abc]; кавычки парсер не снимает. */
    private fun namePredicate(pattern: String): (String) -> Boolean {
        val matcher = GlobPattern(pattern)
        return { path -> matcher.matches(path.substringAfterLast('/').ifEmpty { "/" }) }
    }

    /** Поддерживаются обычные файлы f и каталоги d; символических ссылок в VFS нет. */
    private fun typePredicate(type: String): (String) -> Boolean {
        require(type == "f" || type == "d") { "find: ожидается тип f или d" }
        return { path -> navigation.fileSystem.isDirectory(path) == (type == "d") }
    }

    /** Сохраняет относительный или абсолютный вид заданного пользователем корня поиска. */
    private fun displayPath(root: String, absolute: String, path: String): String {
        val label = root.trimEnd('/').ifEmpty { "/" }
        if (path == absolute) return label
        val suffix = if (absolute == "/") path else path.removePrefix(absolute)
        return if (label == "/") suffix else "$label$suffix"
    }
}
