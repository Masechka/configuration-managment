package nuget

import java.nio.file.Files
import java.nio.file.Path

/** Тестовый граф: одна строка `A: B C`, все узлы объявляются явно. */
class FileRepository private constructor(private val graph: Map<String, List<Dependency>>) :
    Repository {
    /** Возвращает объявленные рёбра; неизвестный пакет считается ошибкой. */
    override fun dependencies(packageName: String): List<Dependency> =
        requireNotNull(graph[packageName]) { "В тестовом графе нет пакета $packageName." }

    /** Позволяет проверить корень, в том числе при нулевой глубине обхода. */
    fun contains(packageName: String): Boolean = packageName in graph

    companion object {
        /** Читает UTF-8 файл без изменения исходных данных. */
        fun read(path: Path): FileRepository = fromText(Files.readString(path))

        /** Разбирает и проверяет весь граф, включая недостижимые вершины. */
        fun fromText(text: String): FileRepository {
            val graph = linkedMapOf<String, List<Dependency>>()
            text.lineSequence().forEachIndexed { index, raw ->
                val line = raw.trim()
                if (line.isNotEmpty() && !line.startsWith('#')) addLine(graph, line, index + 1)
            }
            require(graph.isNotEmpty()) { "Тестовый граф пуст." }
            graph.values.flatten().forEach {
                require(it.name in graph) { "Не объявлен тестовый пакет ${it.name}." }
            }
            return FileRepository(graph)
        }

        /** Проверяет строку и добавляет уникальное объявление пакета. */
        private fun addLine(
            graph: MutableMap<String, List<Dependency>>, line: String, number: Int
        ) {
            require(':' in line) { "Строка $number: нужен разделитель ':'." }
            val name = line.substringBefore(':').trim()
            validateName(name, number)
            require(name !in graph) { "Строка $number: повтор пакета $name." }
            val names = line.substringAfter(':').trim().split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
            names.forEach { validateName(it, number) }
            graph[name] = names.distinct().map { Dependency(it) }
        }

        /** Принимает имена из одной или нескольких больших латинских букв. */
        private fun validateName(name: String, number: Int) {
            require(Regex("[A-Z]+").matches(name)) {
                "Строка $number: имя '$name' должно состоять из больших латинских букв."
            }
        }
    }
}
