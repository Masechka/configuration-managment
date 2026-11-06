package nuget

/** Формирует детерминированный список смежности в порядке BFS. */
object GraphOutput {
    /** Отличает известный лист от непроверенного граничного узла. */
    fun lines(graph: DependencyGraph): List<String> = graph.edges.map { (name, children) ->
        val targets = when {
            name in graph.boundary -> "? [depth-limit]"
            children.isEmpty() -> "(нет)"
            else -> children.joinToString(", ")
        }
        "$name -> $targets"
    }
}
