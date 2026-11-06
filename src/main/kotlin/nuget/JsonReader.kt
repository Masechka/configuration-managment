package nuget

private const val MAX_JSON_DEPTH = 128

/** Рекурсивный спуск с ограничением вложенности и строгой проверкой синтаксиса. */
internal class JsonReader(private val text: String) {
    private var position = 0

    /** Читает один полный документ. */
    fun document(): Any? {
        val value = value(0)
        whitespace()
        require(position == text.length) { "Лишний текст после JSON." }
        return value
    }

    /** Выбирает разбор по первому символу значения. */
    private fun value(depth: Int): Any? {
        require(depth <= MAX_JSON_DEPTH) { "Слишком глубокий JSON." }
        whitespace()
        require(position < text.length) { "Неожиданный конец JSON." }
        return when (text[position]) {
            '{' -> objectValue(depth + 1)
            '[' -> arrayValue(depth + 1)
            '"' -> stringValue()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> numberValue()
        }
    }

    /** Читает объект, запрещая неоднозначные повторяющиеся ключи. */
    private fun objectValue(depth: Int): Map<String, Any?> {
        expect('{')
        val result = linkedMapOf<String, Any?>()
        if (consume('}')) return result
        do {
            whitespace()
            val key = stringValue()
            require(key !in result) { "Повтор ключа JSON: $key" }
            expect(':')
            result[key] = value(depth)
        } while (consume(','))
        expect('}')
        return result
    }

    /** Читает массив JSON с сохранением порядка элементов. */
    private fun arrayValue(depth: Int): List<Any?> {
        expect('[')
        val result = mutableListOf<Any?>()
        if (consume(']')) return result
        do { result += value(depth) } while (consume(','))
        expect(']')
        return result
    }

    /** Читает строку и запрещает неэкранированные управляющие символы. */
    private fun stringValue(): String {
        expect('"')
        val result = StringBuilder()
        while (position < text.length) {
            val char = text[position++]
            if (char == '"') return result.toString()
            require(char >= ' ') { "Управляющий символ в JSON-строке." }
            result.append(if (char == '\\') escaped() else char)
        }
        throw IllegalArgumentException("Незавершённая JSON-строка.")
    }

    /** Декодирует стандартные JSON escape-последовательности. */
    private fun escaped(): Char {
        require(position < text.length) { "Незавершённый escape JSON." }
        val char = text[position++]
        val escapes = mapOf(
            '"' to '"', '\\' to '\\', '/' to '/', 'b' to '\b', 'f' to '\u000c',
            'n' to '\n', 'r' to '\r', 't' to '\t'
        )
        if (char == 'u') return unicode()
        return requireNotNull(escapes[char]) { "Некорректный escape JSON: $char" }
    }

    /** Декодирует четыре шестнадцатеричных символа Unicode. */
    private fun unicode(): Char {
        val width = 4
        require(position + width <= text.length) { "Незавершённый Unicode JSON." }
        val digits = text.substring(position, position + width)
        require(digits.all { it in "0123456789abcdefABCDEF" }) {
            "Некорректный Unicode JSON."
        }
        position += width
        val radix = 16
        return requireNotNull(digits.toIntOrNull(radix)) { "Некорректный Unicode JSON." }.toChar()
    }

    /** Читает число по грамматике JSON без потери точности. */
    private fun numberValue(): java.math.BigDecimal {
        val pattern = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
        val match = requireNotNull(pattern.find(text, position)) { "Некорректное значение JSON." }
        require(match.range.first == position) { "Некорректное число JSON." }
        position += match.value.length
        return match.value.toBigDecimal()
    }

    /** Читает фиксированное слово true, false или null. */
    private fun literal(token: String, result: Any?): Any? {
        require(text.startsWith(token, position)) { "Некорректное значение JSON." }
        position += token.length
        return result
    }

    /** Пропускает разрешённые JSON-пробелы. */
    private fun whitespace() {
        while (position < text.length && text[position] in " \t\r\n") position++
    }

    /** Условно поглощает разделитель после пробелов. */
    private fun consume(char: Char): Boolean {
        whitespace()
        if (text.getOrNull(position) != char) return false
        position++
        return true
    }

    /** Требует заданный разделитель. */
    private fun expect(char: Char) {
        require(consume(char)) { "Ожидался символ '$char' в позиции $position." }
    }
}
