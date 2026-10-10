package shell

/** Сопоставляет glob с именем как строкой, включая имя виртуального корня /. */
internal class GlobPattern(pattern: String) {
    private val regex = Regex(compile(pattern), RegexOption.DOT_MATCHES_ALL)

    /** Проверяет полное совпадение имени, не интерпретируя / как разделитель пути. */
    fun matches(name: String): Boolean = regex.matches(name)

    /** Переводит *, ?, классы и диапазоны в регулярное выражение, экранируя литералы. */
    private fun compile(pattern: String): String {
        val result = StringBuilder()
        var index = 0
        while (index < pattern.length) {
            val symbol = pattern[index++]
            when (symbol) {
                '*' -> result.append(".*")
                '?' -> result.append('.')
                '[' -> {
                    val end = pattern.indexOf(']', index)
                    require(end > index) { "find: незавершённый или пустой класс glob" }
                    result.append(characterClass(pattern.substring(index, end)))
                    index = end + 1
                }
                else -> result.append(Regex.escape(symbol.toString()))
            }
        }
        return result.toString()
    }

    /** Внутри класса ! означает отрицание, а дефис сохраняет смысл диапазона. */
    private fun characterClass(content: String): String {
        val negate = content.startsWith('!')
        val characters = if (negate) content.drop(1) else content
        require(characters.isNotEmpty()) { "find: пустой класс glob" }
        val escaped = characters.map { symbol ->
            if (symbol in "\\[^&") "\\$symbol" else symbol.toString()
        }.joinToString("")
        val prefix = if (negate) "^" else ""
        return "[$prefix$escaped]"
    }
}
