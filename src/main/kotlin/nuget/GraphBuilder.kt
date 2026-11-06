package nuget

import java.util.Locale

private const val MIN_DEPTH = 0

/** Строит ориентированный граф рекурсивным BFS по уровням. */
class GraphBuilder(private val repository: Repository) {
    /** Отмечает корень глубиной 0; каждый доступный пакет запрашивается не более одного раза. */
    fun build(root: String, maxDepth: Int): DependencyGraph {
        require(maxDepth >= MIN_DEPTH) { "Глубина не может быть отрицательной." }
        val state = Traversal(root, maxDepth)
        breadthFirst(listOf(root), 0, state)
        return DependencyGraph(
            root, state.edges.toMap(), state.depths.toMap(),
            state.depths.filterValues { it == maxDepth }.keys
        )
    }

    /** Рекурсивно обрабатывает весь текущий фронт перед переходом на следующий уровень. */
    private tailrec fun breadthFirst(frontier: List<String>, depth: Int, state: Traversal) {
        if (frontier.isEmpty() || depth >= state.maxDepth) return
        val next = mutableListOf<String>()
        frontier.forEach { parent ->
            val children = repository.dependencies(parent)
                .map { state.canonical(it.name) }.distinct()
            state.edges[parent] = children
            children.forEach { child ->
                if (child !in state.depths) {
                    state.depths[child] = depth + 1
                    state.edges[child] = emptyList()
                    next += child
                }
            }
        }
        breadthFirst(next, depth + 1, state)
    }

    /** Хранит состояние одного обхода; новый build не наследует посещённые узлы. */
    private class Traversal(root: String, val maxDepth: Int) {
        val edges = linkedMapOf(root to emptyList<String>())
        val depths = linkedMapOf(root to 0)
        private val names = mutableMapOf(root.lowercase(Locale.ROOT) to root)

        /** Объединяет NuGet-имена без учёта регистра, сохраняя первое написание. */
        fun canonical(name: String): String = names.getOrPut(name.lowercase(Locale.ROOT)) { name }
    }
}
