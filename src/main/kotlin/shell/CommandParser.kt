package shell

/** Простой парсер без раскрытия переменных и обработки кавычек. */
object CommandParser {
    /** Разделяет строку по пробелам, возвращает null для пустого ввода. */
    fun parse(line: String): Command? {
        val parts = line.split(' ').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        return Command(parts.first(), parts.drop(1))
    }
}
