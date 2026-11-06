package nuget

/** Показывает граф ASCII-ветвями, помечая циклы и общие узлы ссылкой [seen]. */
object AsciiTree {
    /** Использует явный стек для вывода; сам граф получен рекурсивным BFS. */
    fun render(graph: DependencyGraph): List<String> {
        val lines = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<Frame>()
        stack.addLast(Frame(graph.root, "", ""))
        while (stack.isNotEmpty()) {
            val frame = stack.removeLast()
            val repeated = !visited.add(frame.name)
            lines += frame.prefix + frame.branch + frame.name + suffix(graph, frame.name, repeated)
            if (!repeated) appendChildren(stack, graph, frame)
        }
        return lines
    }

    /** Помечает повторные и нераскрытые из-за глубины вершины. */
    private fun suffix(graph: DependencyGraph, name: String, repeated: Boolean): String = when {
        repeated -> " [seen]"
        name in graph.boundary -> " [depth-limit]"
        else -> ""
    }

    /** Кладёт дочерние строки в обратном порядке, чтобы сохранить порядок рёбер. */
    private fun appendChildren(stack: ArrayDeque<Frame>, graph: DependencyGraph, frame: Frame) {
        val children = graph.edges.getValue(frame.name)
        val prefix = frame.prefix + when (frame.branch) {
            "|-- " -> "|   "
            "`-- " -> "    "
            else -> ""
        }
        children.indices.reversed().forEach { index ->
            val branch = if (index == children.lastIndex) "`-- " else "|-- "
            stack.addLast(Frame(children[index], prefix, branch))
        }
    }

    /** Строка дерева и префикс вертикальных ветвей её родителей. */
    private data class Frame(val name: String, val prefix: String, val branch: String)
}
